package com.mpdl.labcam.mvvm.ui.fragment

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.webkit.MimeTypeMap
import android.widget.*
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.mlkit.common.MlKitException
import com.mpdl.labcam.R
import com.mpdl.labcam.detecort.TextRecognitionProcessor
import com.mpdl.labcam.detecort.VisionImageProcessor
import com.mpdl.labcam.mvvm.repository.bean.SaveDirectoryBean
import com.mpdl.labcam.mvvm.ui.activity.MainActivity
import com.mpdl.labcam.mvvm.ui.activity.MainActivity.Companion.EVENT_UPLOAD_OVER
import com.mpdl.labcam.mvvm.ui.adapter.MyImageAnalyzer
import com.mpdl.labcam.mvvm.ui.widget.CustomPopupWindow
import com.mpdl.labcam.mvvm.ui.widget.CustomPreviewView
import com.mpdl.labcam.mvvm.ui.widget.FileSelectorDialog
import com.mpdl.labcam.mvvm.ui.widget.GraphicOverlay
import com.mpdl.labcam.mvvm.vm.CameraViewModel
import com.mpdl.mvvm.base.BaseFragment
import com.mpdl.mvvm.common.Preference
import kotlinx.android.synthetic.main.fragment_camera.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.simple.eventbus.EventBus
import org.simple.eventbus.Subscriber
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class CameraFragment: BaseFragment<CameraViewModel>() {
    override fun initViewModel(): CameraViewModel = getViewModel()

    private var displayId: Int = -1
    private lateinit var viewFinder: CustomPreviewView
    private var graphicOverlay: GraphicOverlay? = null
    private var analysisUseCase: ImageAnalysis? = null
    private var imageProcessor: VisionImageProcessor? = null
    private var needUpdateGraphicOverlayImageSourceInfo = false
    private var cameraSelector: CameraSelector? = null



    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var preview: Preview? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var outputDirectory: File
    private var flashMode:Int = ImageCapture.FLASH_MODE_AUTO
    private var setPopup: CustomPopupWindow? = null
    private var tvDir: TextView? = null

    /** Blocking camera operations are performed using this executor */
    private var cameraExecutor: ExecutorService? = null

    private var firstInit = true


    private var fileSelectorDialog:FileSelectorDialog? = null

    /**
     * We need a display listener for orientation changes that do not trigger a configuration
     * change, for example if we choose to override config change in manifest or for 180-degree
     * orientation changes.
     */
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = view?.let { view ->
            if (displayId == this@CameraFragment.displayId) {
                Timber.d("Rotation changed: ${view.display.rotation}")
                imageCapture?.targetRotation = view.display.rotation
            }
        } ?: Unit
    }

    override fun initView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        var view = inflater.inflate(R.layout.fragment_camera,container,false)
        viewFinder = view.findViewById(R.id.preview_view)
        graphicOverlay = view.findViewById(R.id.graphic_overlay)
        return view
    }

    private val displayManager by lazy {
        requireContext().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    @SuppressLint("RestrictedApi")
    override fun initData(savedInstanceState: Bundle?) {
        EventBus.getDefault().register(this)
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        flashMode = Preference.preferences.getInt(SP_FLASH_MODE,ImageCapture.FLASH_MODE_AUTO);
        cameraExecutor = Executors.newSingleThreadExecutor()
        displayManager.registerDisplayListener(displayListener, null)
        outputDirectory = MainActivity.getOutputDirectory(requireContext())

        initFileSelectorDialog()

        checkDirPath()

        // Wait for the views to be properly laid out
        viewFinder.post {
            // Keep track of the display in which this view is attached
            displayId = viewFinder.display.displayId
            // Set up the camera and its use cases
            setUpCamera()
        }

        // In the background, load latest photo taken (if any) for gallery thumbnail
        lifecycleScope.launch(Dispatchers.IO) {
            outputDirectory.listFiles { file ->
                MainActivity.EXTENSION_WHITELIST.contains(file.extension.toUpperCase(Locale.ROOT))
            }?.max()?.let {
                setGalleryThumbnail(Uri.fromFile(it))
            }
        }

        observe(mViewModel.getDirectoryData()){list->
            fileSelectorDialog?.let {
                it.setData(list)
            }
        }

        observe(mViewModel.getUiState()){
            if (it.uploadUrlSuc){
                fileSelectorDialog?.let {dialog->
                    MainActivity.getSaveDirectory()?.let{saveDir->
                        tvDir?.text = saveDir.repoName+saveDir.path
                    }
                    dialog.dismiss()
                }
            }

            if (it.checkoutDirPathSuc){
                MainActivity.getSaveDirectory()?.let {saveDir->
                    showMessage("upload path: "+saveDir.repoName+saveDir.path)
                    MainActivity.startUpload()
                }
            }

            if (it.showFileDirDialog){
                fileSelectorDialog?.show()
            }

        }

        setClickListener()
    }

    private fun setClickListener(){
        btn_take_picture.setOnClickListener {
            takePicture()
        }

        btn_camera_switch.setOnClickListener {
            lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }
            bindCameraUseCases()
            bindAnalysisUseCase()
        }

        btn_photo_view.setOnClickListener {
            if (true == outputDirectory.listFiles()?.isNotEmpty()) {
                mViewModel.cleanUiState()
                Navigation.findNavController(requireView())
                    .navigate(CameraFragmentDirections
                        .actionCameraFragmentToGalleryFragment(outputDirectory.absolutePath))
            }
        }

        btn_flash.setOnClickListener {
            clickFlash()
        }

        btn_menu.setOnClickListener {
            showPopup()
        }
    }

    /** Initialize CameraX, and prepare to bind the camera use cases  */
    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {
            // CameraProvider
            cameraProvider = cameraProviderFuture.get()

            // Select lensFacing depending on the available cameras
            if (firstInit){
                firstInit = false
                lensFacing = when {
                    hasBackCamera() -> CameraSelector.LENS_FACING_BACK
                    hasFrontCamera() -> CameraSelector.LENS_FACING_FRONT
                    else -> throw IllegalStateException("Back and front camera are unavailable")
                }
            }
            // Enable or disable switching between cameras
            updateCameraSwitchButton()

            // Build and bind the camera use cases
            bindCameraUseCases()
            bindAnalysisUseCase()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    /** Declare and bind preview, capture and analysis use cases */
    private fun bindCameraUseCases() {
        viewFinder.setPreviewViewControl(object: CustomPreviewView.CustomPreviewViewControl{
            override fun getMaxZoom(): Float {
                return camera?.cameraInfo?.zoomState?.value!!.maxZoomRatio
            }

            override fun getMinZoom(): Float {
                return camera?.cameraInfo?.zoomState?.value!!.minZoomRatio
            }

            override fun setZoom(zoom: Float) {
                camera?.cameraControl?.setZoomRatio(zoom)
            }

            override fun getZoom(): Float {
                return camera?.cameraInfo?.zoomState?.value!!.zoomRatio
            }

            override fun focus(x: Float, y: Float) {
                val point = viewFinder.meteringPointFactory.createPoint(x,y)
                val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                    // auto calling cancelFocusAndMetering in 3 seconds
                    .setAutoCancelDuration(3, TimeUnit.SECONDS)
                    .build()
                camera?.cameraControl!!.cancelFocusAndMetering()
                camera?.cameraControl!!.startFocusAndMetering(action).addListener(Runnable {
                    viewFinder.cleanFocus()
                },ContextCompat.getMainExecutor(requireContext()))
            }
        })

        // Get screen metrics used to setup camera for full screen resolution
        val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
        Timber.d( "Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")

        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        Timber.d( "Preview aspect ratio: $screenAspectRatio")

        val rotation = viewFinder.display.rotation

        // CameraProvider
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        // CameraSelector
        cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        // Preview
        preview = Preview.Builder()
            // We request aspect ratio but no resolution
            .setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation
            .setTargetRotation(rotation)
            .build()


        // ImageCapture
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .setFlashMode(flashMode)
            .build()
        setFlashBtn()

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()
        try {
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector!!, preview, imageCapture)
            preview?.setSurfaceProvider(viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            exc.printStackTrace()
        }
    }


    private fun bindAnalysisUseCase() {
        if (true){
            return
        }
        cameraProvider?.let {
            if (analysisUseCase != null) {
                it.unbind(analysisUseCase)
            }

            if (imageProcessor != null) {
                imageProcessor!!.stop()
            }

            imageProcessor = try {
                TextRecognitionProcessor(requireContext())
            }catch (e: Exception){
                showMessage("Can not create image processor: ${e.localizedMessage}")
                return
            }

            val builder = ImageAnalysis.Builder()
            builder.setTargetRotation(viewFinder.display.rotation)
            analysisUseCase = builder.build()
            needUpdateGraphicOverlayImageSourceInfo = true
            analysisUseCase?.setAnalyzer(ContextCompat.getMainExecutor(requireContext()),
                ImageAnalysis.Analyzer { imageProxy: ImageProxy ->
                    if (needUpdateGraphicOverlayImageSourceInfo) {
                        val isImageFlipped =
                            lensFacing == CameraSelector.LENS_FACING_FRONT
                        val rotationDegrees =
                            imageProxy.imageInfo.rotationDegrees
                        if (rotationDegrees == 0 || rotationDegrees == 180) {
                            graphicOverlay!!.setImageSourceInfo(
                                imageProxy.width, imageProxy.height, isImageFlipped
                            )
                        } else {
                            graphicOverlay!!.setImageSourceInfo(
                                imageProxy.height, imageProxy.width, isImageFlipped
                            )
                        }
                        needUpdateGraphicOverlayImageSourceInfo = false
                    }

                    try {
                        imageProcessor!!.processImageProxy(imageProxy, graphicOverlay)
                    } catch (e: MlKitException) {
                        Timber.e("Failed to process image. Error: ${e.localizedMessage}")
//                        showMessage(e.localizedMessage)
                    }
            })
            it.bindToLifecycle(this, cameraSelector!!, analysisUseCase)
        }
    }

    private var lastTakePictureTime: Long = 0

    private fun takePicture() {
        var nowTime =  System.currentTimeMillis()
        if (nowTime - lastTakePictureTime < 500){
            return
        }
        lastTakePictureTime = nowTime
        // Get a stable reference of the modifiable image capture use case
        imageCapture?.let { imageCapture ->
            // Create output file to hold the image
            val photoFile = createFile(outputDirectory, FILENAME, PHOTO_EXTENSION)
            // Setup image capture metadata
            val metadata = ImageCapture.Metadata().apply {
                // Mirror image when using the front camera
                isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
            }

            // Create output options object which contains file + metadata
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
                .setMetadata(metadata).build()

            // Setup image capture listener which is triggered after photo has been taken
            imageCapture.takePicture(outputOptions, cameraExecutor!!, object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        exc.printStackTrace()
                        Timber.e("Photo capture failed: ${exc.message}")
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                        setGalleryThumbnail(savedUri)

                        val mimeType = MimeTypeMap.getSingleton()
                            .getMimeTypeFromExtension(savedUri.toFile().extension)
                        MediaScannerConnection.scanFile(
                            context,
                            arrayOf(savedUri.toFile().absolutePath),
                            arrayOf(mimeType)
                        ) { _, uri ->
                            Timber.d("Image capture scanned into media store: $uri")
                        }
                        Timber.d( "Photo capture succeeded: $savedUri")
                        MainActivity.startUpload()
                    }
                })
        }
    }

    private fun setGalleryThumbnail(uri: Uri) {
        btn_photo_view.post {
            Glide.with(btn_photo_view)
                .load(uri)
                .apply(RequestOptions.circleCropTransform())
                .into(btn_photo_view)
        }
    }

    private fun clickFlash(){
        Timber.d("setFlashMode $flashMode")
        when (flashMode) {
            ImageCapture.FLASH_MODE_AUTO -> {
                flashMode = ImageCapture.FLASH_MODE_ON
            }
            ImageCapture.FLASH_MODE_ON -> {
                flashMode = ImageCapture.FLASH_MODE_OFF
            }
            ImageCapture.FLASH_MODE_OFF -> {
                flashMode = ImageCapture.FLASH_MODE_AUTO
            }
        }
        imageCapture!!.flashMode = flashMode
        setFlashBtn()
        Preference.preferences.edit().putInt(SP_FLASH_MODE,flashMode).apply()
    }

    private fun setFlashBtn(){
        when(flashMode){
            ImageCapture.FLASH_MODE_AUTO -> {
                btn_flash.setImageResource(R.mipmap.flash_auto)
            }
            ImageCapture.FLASH_MODE_ON -> {
                btn_flash.setImageResource(R.mipmap.flash_on)
            }
            ImageCapture.FLASH_MODE_OFF -> {
                btn_flash.setImageResource(R.mipmap.flash_off)
            }
        }
    }

    private fun checkDirPath(){
        if (!MainActivity.isCheckDirPath){
            MainActivity.isCheckDirPath = true
            //没有网络
            if (MainActivity.curNetworkType == -1){
                MainActivity.getSaveDirectory()?.let{
                    showMessage("upload path: "+it.repoName+it.path)
                }
                return
            }
            val saveDirectoryBean =MainActivity.getSaveDirectory()
            if (saveDirectoryBean == null){
                fileSelectorDialog?.show()
            }else{
                mViewModel.checkDirPath(saveDirectoryBean)
            }
        }
    }


    private fun initFileSelectorDialog(){
        fileSelectorDialog = FileSelectorDialog(requireContext())
        fileSelectorDialog!!.setCanceledOnTouchOutside(false)
        fileSelectorDialog!!.setFileSelectorListener(object : FileSelectorDialog.FileSelectorListener {
            override fun onBack(bean: SaveDirectoryBean?) {
                getDir(bean)
            }

            override fun onSave(bean: SaveDirectoryBean?) {
                if (bean == null){
                    showMessage("Please select a directory")
                }else{
                    MainActivity.setSaveDirectory(bean)
                    mViewModel.getUploadLink(bean)
                }
            }

            override fun onItemClick(bean: SaveDirectoryBean?) {
                getDir(bean)
            }
        })
    }


    private fun showPopup(){
        if (setPopup == null){
            setPopup = CustomPopupWindow
                .builder()
                .contentView(CustomPopupWindow.inflateView(requireActivity(),R.layout.popup_set))
                .customListener {
                    tvDir = it.findViewById<TextView>(R.id.tv_dir)
                    val tvNetwork = it.findViewById<TextView>(R.id.tv_network)
                    val spNetwork = it.findViewById<Spinner>(R.id.sp_network)

                    MainActivity.getSaveDirectory()?.let{saveDir->
                        tvDir!!.text = saveDir.repoName+saveDir.path
                    }
                    tvDir!!.setOnClickListener {
                        fileSelectorDialog!!.show()
                    }
                    tvNetwork.text = resources.getStringArray(R.array.upload_network)[MainActivity.getUploadNetwork()]

                    it.findViewById<ImageView>(R.id.iv_dir).setOnClickListener {
                        fileSelectorDialog!!.show()
                    }

                    tvNetwork.setOnClickListener {
                        spNetwork.performClick()
                    }

                    it.findViewById<ImageView>(R.id.iv_network).setOnClickListener {
                        spNetwork.performClick()
                    }

                    it.findViewById<TextView>(R.id.btn_logout).setOnClickListener {
                        setPopup!!.dismiss()
                        MainActivity.loginOut()
                        Navigation.findNavController(requireView())
                            .navigate(CameraFragmentDirections.actionCameraFragmentToLoginFragment())
                    }

                    it.findViewById<LinearLayout>(R.id.btn_dismiss).setOnClickListener {
                        setPopup!!.dismiss()
                    }


                    spNetwork.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                        override fun onNothingSelected(p0: AdapterView<*>?) {
                        }

                        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                            MainActivity.setUploadNetwork(position)
                            tvNetwork.text = resources.getStringArray(R.array.upload_network)[position]
                        }
                    }

                }.build()
            setPopup!!.animationStyle = R.style.popwindow_anim_style
            setPopup!!.height = WindowManager.LayoutParams.WRAP_CONTENT
        }
        setPopup!!.showAtLocation(requireView(), Gravity.TOP, 0, 0);
        //AutoSizeUtils.dp2px(requireContext(),21f)
    }

    private fun getDir(bean: SaveDirectoryBean?){
        if (bean == null){
            mViewModel.getRepos()
        }else {
            mViewModel.getDir(bean)
        }
    }


    /** Enabled or disabled a button to switch cameras depending on the available cameras */
    private fun updateCameraSwitchButton() {
        try {
            btn_camera_switch.isEnabled = hasBackCamera() && hasFrontCamera()
        } catch (exception: CameraInfoUnavailableException) {
            btn_camera_switch.isEnabled = false
        }
    }

    /** Returns true if the device has an available back camera. False otherwise */
    private fun hasBackCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }

    /** Returns true if the device has an available front camera. False otherwise */
    private fun hasFrontCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setUpCamera()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        EventBus.getDefault().unregister(this)
        // Shut down our background executor
        cameraExecutor!!.shutdown()
        displayManager.unregisterDisplayListener(displayListener)
    }

    var progressDialog: ProgressDialog? = null
    override fun showLoading() {
        if (progressDialog == null){
            progressDialog = ProgressDialog(requireContext())
            progressDialog!!.setCanceledOnTouchOutside(false)
            progressDialog!!.setMessage("LOAD...")
        }
        progressDialog?.show()
        val window: Window? = progressDialog!!.window
        val lp = window!!.attributes
        lp.gravity = Gravity.CENTER
        lp.width = (window.windowManager.defaultDisplay.width * 0.8).toInt()
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
        progressDialog!!.getWindow()!!.setAttributes(lp)
    }

    override fun hideLoading() {
        progressDialog?.dismiss()
    }

    @Subscriber(tag = EVENT_UPLOAD_OVER)
    fun uploadOver(msg:String){
        Timber.d("上传 完成通知")
        if (outputDirectory.listFiles().isEmpty()){
            btn_photo_view.setImageResource(R.drawable.ic_photo)
        }
    }

    companion object {
        private const val SP_FLASH_MODE = "sp_flash_mode";
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_EXTENSION = ".jpg"
        private const val VIDEO_EXTENSION = ".mp4"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0

        /** Helper function used to create a timestamped file */
        private fun createFile(baseFolder: File, format: String, extension: String) =
            File(baseFolder, SimpleDateFormat(format, Locale.GERMAN)
                .format(System.currentTimeMillis()) + extension)
    }

}