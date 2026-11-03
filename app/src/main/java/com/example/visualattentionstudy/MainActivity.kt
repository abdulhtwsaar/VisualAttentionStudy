@file:OptIn(androidx.camera.core.ExperimentalGetImage::class)
package com.example.visualattentionstudy
import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.delay
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

val faceDetectorOptions = FaceDetectorOptions.Builder()
    .setPerformanceMode(
        FaceDetectorOptions.PERFORMANCE_MODE_FAST
    )
    .setClassificationMode(
        FaceDetectorOptions.CLASSIFICATION_MODE_ALL
    )
    .setLandmarkMode(
        FaceDetectorOptions.LANDMARK_MODE_ALL
    )
    .enableTracking()
    .build()

val faceDetector: FaceDetector =
    FaceDetection.getClient(faceDetectorOptions)



data class CalibrationPointData(

    // Real StudyPosition ID (1-25)
    val positionId: Int,

    // Normalized target position in the 5x5 grid
    val targetX: Float,
    val targetY: Float,

    // Actual calibration-dot center on the phone screen (px)
    val targetScreenX: Float,
    val targetScreenY: Float,

    // Time relative to Calibration Video (ms)
    val startMs: Long,
    val endMs: Long
)
fun median(values: List<Float>): Float {

    if (values.isEmpty()) {
        return 0f
    }

    val sorted = values.sorted()
    val middle = sorted.size / 2

    return if (sorted.size % 2 == 0) {

        (sorted[middle - 1] + sorted[middle]) / 2f

    } else {

        sorted[middle]
    }
}
data class ImageEvent(
    val imageName: String,
    val startMs: Long,
    val endMs: Long,
    val positionId: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)
data class StudyPosition(
    val id: Int,
    val x: Float,
    val y: Float
)
val studyPositions = listOf(
    StudyPosition(1, 0.00f, 0.00f),
    StudyPosition(2, 0.25f, 0.00f),
    StudyPosition(3, 0.50f, 0.00f),
    StudyPosition(4, 0.75f, 0.00f),
    StudyPosition(5, 1.00f, 0.00f),

    StudyPosition(6, 0.00f, 0.25f),
    StudyPosition(7, 0.25f, 0.25f),
    StudyPosition(8, 0.50f, 0.25f),
    StudyPosition(9, 0.75f, 0.25f),
    StudyPosition(10, 1.00f, 0.25f),

    StudyPosition(11, 0.00f, 0.50f),
    StudyPosition(12, 0.25f, 0.50f),
    StudyPosition(13, 0.50f, 0.50f),
    StudyPosition(14, 0.75f, 0.50f),
    StudyPosition(15, 1.00f, 0.50f),

    StudyPosition(16, 0.00f, 0.75f),
    StudyPosition(17, 0.25f, 0.75f),
    StudyPosition(18, 0.50f, 0.75f),
    StudyPosition(19, 0.75f, 0.75f),
    StudyPosition(20, 1.00f, 0.75f),

    StudyPosition(21, 0.00f, 1.00f),
    StudyPosition(22, 0.25f, 1.00f),
    StudyPosition(23, 0.50f, 1.00f),
    StudyPosition(24, 0.75f, 1.00f),
    StudyPosition(25, 1.00f, 1.00f)
)

data class QuizAnswer(
    val questionNumber: Int,
    val imageName: String,
    val shownDuringTest: Boolean,
    val userAnswer: Boolean,
    val correct: Boolean
)

data class StudyImage(
    val imageRes: Int,
    val imageName: String
)

val studyImages = listOf(
    StudyImage(R.drawable.auto, "Auto"),
    StudyImage(R.drawable.ball, "Ball"),
    StudyImage(R.drawable.stift, "Stift"),
    StudyImage(R.drawable.apfel, "Apfel"),
    StudyImage(R.drawable.fahrad, "Fahrrad"),
    StudyImage(R.drawable.hund, "Hund"),
    StudyImage(R.drawable.katze, "Katze"),
    StudyImage(R.drawable.tasse, "Tasse"),
    StudyImage(R.drawable.uhr, "Uhr"),
    StudyImage(R.drawable.schlussel, "Schlüssel"),
    StudyImage(R.drawable.buch, "Buch"),
    StudyImage(R.drawable.bananne, "Banane"),
    StudyImage(R.drawable.schuhe, "Schuhe"),
    StudyImage(R.drawable.wasserflasche, "Wasserflasche"),
    StudyImage(R.drawable.brille, "Brille"),
    StudyImage(R.drawable.blummen, "Blumen"),
    StudyImage(R.drawable.telefon, "Telefon"),
    StudyImage(R.drawable.loeffel, "Löffel"),
    StudyImage(R.drawable.haus, "Haus"),
    StudyImage(R.drawable.baum, "Baum")
)
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            MaterialTheme {
                VisualAttentionApp()
            }
        }
    }
}

@Composable
fun VisualAttentionApp() {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        Log.d(
            "APP_LIFECYCLE",
            "VisualAttentionApp CREATED"
        )
    }

    var shownImages by remember {
        mutableStateOf<Set<Int>>(emptySet())
    }

    var currentScreen by rememberSaveable {
        mutableStateOf("participant")
    }
    LaunchedEffect(currentScreen) {
        Log.d(
            "APP_LIFECYCLE",
            "CURRENT SCREEN = $currentScreen"
        )
    }

    var savedCalibrationResults by remember {
        mutableStateOf<List<CalibrationPointData>>(emptyList())
    }

    var participantId by remember {
        mutableStateOf("")
    }

    var age by remember {
        mutableStateOf("")
    }

    var gender by remember {
        mutableStateOf("")
    }

    when (currentScreen) {

        // --------------------------------
        // PARTICIPANT
        // --------------------------------

        "participant" -> ParticipantScreen(
            participantId = participantId,
            age = age,
            gender = gender,
            onParticipantIdChange = {
                participantId = it
            },
            onAgeChange = {
                age = it
            },
            onGenderChange = {
                gender = it
            },
            onContinue = {
                currentScreen = "calibration"
            }
        )

        // --------------------------------
        // CALIBRATION
        // --------------------------------

        "calibration" -> CalibrationScreen(
            participantId = participantId,
            onBack = {
                currentScreen = "participant"
            },
            onStartTest = { results ->

                savedCalibrationResults = results

                saveCalibrationResultsToXlsx(
                    context = context,
                    participantId = participantId,
                    calibrationResults = results
                )

                // بعد Calibration نذهب إلى Testanweisungen
                currentScreen = "instructions"
            }
        )

        // --------------------------------
        // TEST INSTRUCTIONS
        // --------------------------------

        "instructions" -> InstructionScreen(
            onBack = {
                currentScreen = "calibration"
            },
            onContinue = {
                currentScreen = "test"
            }
        )

        // --------------------------------
        // TEST
        // --------------------------------

        "test" -> TestScreen(
            participantId = participantId,
            onTestFinished = { images ->

                shownImages = images.toSet()

                currentScreen = "quiz"
            }
        )

        // --------------------------------
        // QUIZ
        // --------------------------------

        "quiz" -> QuizScreen(
            participantId = participantId,
            age = age,
            gender = gender,
            shownImages = shownImages,
            onFinish = {
                currentScreen = "result"
            }
        )

        // --------------------------------
        // RESULT
        // --------------------------------

        "result" -> ResultScreen(
            participantId = participantId
        )
    }
}
fun saveQuizResultsToXlsx(
    context: android.content.Context,
    participantId: String,
    age: String,
    gender: String,
    answers: List<QuizAnswer>
): String {

    val timestamp = SimpleDateFormat(
        "yyyyMMdd_HHmmss",
        Locale.getDefault()
    ).format(Date())

    val safeParticipantId = participantId.replace(
        Regex("[^A-Za-z0-9_-]"),
        "_"
    )

    val fileName =
        "${safeParticipantId}_Quiz_$timestamp.xlsx"

    val correctAnswers =
        answers.count { it.correct }

    // Create Excel workbook
    val workbook = XSSFWorkbook()

    // Create sheet
    val sheet = workbook.createSheet("Quiz Results")

    // --------------------------------
    // HEADER STYLE
    // --------------------------------

    val headerStyle = workbook.createCellStyle()

    val headerFont = workbook.createFont().apply {
        bold = true
    }

    headerStyle.setFont(headerFont)

    headerStyle.alignment =
        HorizontalAlignment.CENTER

    headerStyle.verticalAlignment =
        org.apache.poi.ss.usermodel.VerticalAlignment.CENTER

    headerStyle.fillForegroundColor =
        IndexedColors.GREY_25_PERCENT.index

    headerStyle.fillPattern =
        FillPatternType.SOLID_FOREGROUND

    // --------------------------------
    // DATA STYLE
    // --------------------------------

    val dataStyle = workbook.createCellStyle().apply {

        alignment =
            HorizontalAlignment.CENTER

        verticalAlignment =
            org.apache.poi.ss.usermodel.VerticalAlignment.CENTER
    }

    // --------------------------------
    // SCORE STYLE
    // --------------------------------

    val scoreStyle = workbook.createCellStyle().apply {

        alignment =
            HorizontalAlignment.CENTER

        verticalAlignment =
            org.apache.poi.ss.usermodel.VerticalAlignment.CENTER

        fillForegroundColor =
            IndexedColors.GREY_25_PERCENT.index

        fillPattern =
            FillPatternType.SOLID_FOREGROUND

        setFont(headerFont)
    }

    // --------------------------------
    // HEADER
    // --------------------------------
    val headers = listOf(
        "Participant",
        "Age",
        "Gender",
        "Question Number",
        "Image",
        "Shown During Test",
        "User Answer",
        "Correct"
    )

    val headerRow = sheet.createRow(0)

    headers.forEachIndexed { index, title ->

        val cell = headerRow.createCell(index)

        cell.setCellValue(title)

        cell.cellStyle = headerStyle
    }

    // --------------------------------
    // QUIZ DATA
    // --------------------------------

    answers.forEachIndexed { index, answer ->

        val row = sheet.createRow(index + 1)

        row.createCell(0).setCellValue(
            safeParticipantId
        )

        row.createCell(1).setCellValue(
            age
        )

        row.createCell(2).setCellValue(
            gender
        )

        row.createCell(3).setCellValue(
            answer.questionNumber.toDouble()
        )

        row.createCell(4).setCellValue(
            answer.imageName
        )

        row.createCell(5).setCellValue(
            if (answer.shownDuringTest) "Ja" else "Nein"
        )

        row.createCell(6).setCellValue(
            if (answer.userAnswer) "Ja" else "Nein"
        )

        row.createCell(7).setCellValue(
            if (answer.correct) "Ja" else "Nein"
        )

        // Center all cells
        for (columnIndex in headers.indices) {
            row.getCell(columnIndex).cellStyle =
                dataStyle
        }
    }

    // --------------------------------
    // SCORE
    // --------------------------------

    val scoreRowIndex =
        answers.size + 2

    val scoreRow =
        sheet.createRow(scoreRowIndex)

    scoreRow.createCell(0).setCellValue(
        "Score"
    )

    scoreRow.createCell(1).setCellValue(
        correctAnswers.toDouble()
    )

    scoreRow.createCell(2).setCellValue(
        "Total"
    )

    scoreRow.createCell(3).setCellValue(
        answers.size.toDouble()
    )

    for (columnIndex in 0..3) {

        scoreRow.getCell(columnIndex).cellStyle =
            scoreStyle
    }

    // --------------------------------
    // COLUMN WIDTHS
    // --------------------------------

    sheet.setColumnWidth(
        0,
        18 * 256
    ) // Participant

    sheet.setColumnWidth(
        1,
        12 * 256
    ) // Age

    sheet.setColumnWidth(
        2,
        18 * 256
    ) // Gender

    sheet.setColumnWidth(
        3,
        15 * 256
    ) // Question

    sheet.setColumnWidth(
        4,
        18 * 256
    ) // Image

    sheet.setColumnWidth(
        5,
        24 * 256
    ) // Shown During Test

    sheet.setColumnWidth(
        6,
        20 * 256
    ) // User Answer

    sheet.setColumnWidth(
        7,
        16 * 256
    ) // Correct

    // Keep header visible
    sheet.createFreezePane(
        0,
        1
    )

    // --------------------------------
    // SAVE XLSX
    // --------------------------------

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

        val values =
            ContentValues().apply {

                put(
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    fileName
                )

                put(
                    MediaStore.MediaColumns.MIME_TYPE,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                )

                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS +
                            "/VisualAttentionStudy"
                )
            }

        val uri =
            context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                values
            )

        if (uri != null) {

            context.contentResolver
                .openOutputStream(uri)
                ?.use { outputStream ->

                    workbook.write(
                        outputStream
                    )
                }

            workbook.close()

            return uri.toString()
        }

    } else {

        val directory =
            File(
                context.getExternalFilesDir(
                    Environment.DIRECTORY_DOCUMENTS
                ),
                "VisualAttentionStudy"
            )

        directory.mkdirs()

        val file =
            File(
                directory,
                fileName
            )

        file.outputStream()
            .use { outputStream ->

                workbook.write(
                    outputStream
                )
            }

        workbook.close()

        return file.absolutePath
    }

    workbook.close()

    return ""
}
@Composable
fun ParticipantScreen(
    participantId: String,
    age: String,
    gender: String,
    onParticipantIdChange: (String) -> Unit,
    onAgeChange: (String) -> Unit,
    onGenderChange: (String) -> Unit,
    onContinue: () -> Unit
) {

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = "Visual Attention Study",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Teilnehmerdaten",
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(35.dp))

            OutlinedTextField(
                value = participantId,
                onValueChange = onParticipantIdChange,
                label = { Text("Teilnehmer-ID") },
                placeholder = { Text("z. B. P001") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = age,
                onValueChange = onAgeChange,
                label = { Text("Alter") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    text = "Geschlecht",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    RadioButton(
                        selected = gender == "Männlich",
                        onClick = {
                            onGenderChange("Männlich")
                        }
                    )

                    Text(
                        text = "Männlich",
                        modifier = Modifier.clickable {
                            onGenderChange("Männlich")
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    RadioButton(
                        selected = gender == "Weiblich",
                        onClick = {
                            onGenderChange("Weiblich")
                        }
                    )

                    Text(
                        text = "Weiblich",
                        modifier = Modifier.clickable {
                            onGenderChange("Weiblich")
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    RadioButton(
                        selected = gender == "Divers",
                        onClick = {
                            onGenderChange("Divers")
                        }
                    )

                    Text(
                        text = "Divers",
                        modifier = Modifier.clickable {
                            onGenderChange("Divers")
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                enabled = participantId.isNotBlank() &&
                        age.isNotBlank() &&
                        gender.isNotBlank()
            ) {
                Text(
                    text = "Weiter",
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
fun InstructionScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit
) {

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = "Testanweisungen",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = """
Der Test dauert etwa 3 Minuten.

Während des Tests sehen Sie ein bewegliches Objekt auf dem Bildschirm.

Bitte verfolgen Sie dieses Objekt aufmerksam mit Ihren Augen.

Während des Tests werden zusätzlich kurz Bilder eingeblendet.

Versuchen Sie, sich diese Bilder zu merken.

Nach dem Test werden Ihnen Fragen zu den gezeigten Bildern gestellt.
                """.trimIndent(),
                fontSize = 17.sp
            )

            Spacer(modifier = Modifier.height(35.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
            ) {
                Text(
                    text = "Test starten"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Zurück")
            }
        }
    }
}
fun createMediaPipeFaceLandmarker(
    context: android.content.Context,
    onProcessingFinished: () -> Unit = {},
    onResult: (FaceLandmarkerResult) -> Unit
): FaceLandmarker {

    val baseOptions =
        BaseOptions.builder()
            .setModelAssetPath(
                "face_landmarker.task"
            )
            .build()

    val options =
        FaceLandmarker.FaceLandmarkerOptions
            .builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(
                RunningMode.LIVE_STREAM
            )
            .setNumFaces(1)
            .setMinFaceDetectionConfidence(0.5f)
            .setMinFacePresenceConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)

            .setResultListener { result, _ ->

                try {
                    onResult(result)
                } finally {
                    onProcessingFinished()
                }
            }

            .setErrorListener { error ->

                Log.e(
                    "MEDIAPIPE_DEBUG",
                    "MediaPipe error: ${error.message}"
                )

                onProcessingFinished()
            }

            .build()

    return FaceLandmarker.createFromOptions(
        context,
        options
    )
}
@OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun CalibrationFaceAnalyzer(
    participantId: String,
    stopRecording: Boolean,
    onRecordingStarted: () -> Unit,
    onRecordingTimeUpdate: (Long) -> Unit,
    onRecordingFinished: (String) -> Unit,
    onRecordingError: (String) -> Unit
){

    val context = LocalContext.current
    var activeCalibrationRecording by remember {
        mutableStateOf<androidx.camera.video.Recording?>(null)
    }
    var firstCameraTimestampNs by remember {
        mutableLongStateOf(0L)
    }

    var lastCameraTimestampNs by remember {
        mutableLongStateOf(0L)
    }
    val mediaPipeBusy =
        remember {
            java.util.concurrent.atomic.AtomicBoolean(false)
        }


    // =========================================================
    // CAMERA
    // =========================================================

    DisposableEffect(Unit) {

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(
                context
            )

        val mainExecutor =
            ContextCompat.getMainExecutor(
                context
            )

        val analysisExecutor =
            Executors.newSingleThreadExecutor()

        var disposed = false


        cameraProviderFuture.addListener({

            if (!disposed) {

                try {

                    val cameraProvider =
                        cameraProviderFuture.get()

                    val recorder =
                        Recorder.Builder()
                            .setQualitySelector(
                                QualitySelector.from(
                                    Quality.HD
                                )
                            )
                            .build()

                    val videoCapture =
                        VideoCapture.withOutput(
                            recorder
                        )






                    // =================================
                    // BIND CAMERA
                    // =================================

                    cameraProvider
                        .bindToLifecycle(
                            context as ComponentActivity,

                            CameraSelector.DEFAULT_FRONT_CAMERA,

                            videoCapture
                        )
                            val timestamp =
                    SimpleDateFormat(
                        "yyyyMMdd_HHmmss",
                        Locale.getDefault()
                    ).format(Date())

                    val safeParticipantId =
                        participantId.replace(
                            Regex("[^A-Za-z0-9_-]"),
                            "_"
                        )

                    val videoName =
                        "${safeParticipantId}_CalibrationVideo_$timestamp"

                    val contentValues =
                        ContentValues().apply {

                            put(
                                MediaStore.MediaColumns.DISPLAY_NAME,
                                videoName
                            )

                            put(
                                MediaStore.MediaColumns.MIME_TYPE,
                                "video/mp4"
                            )

                            if (
                                Build.VERSION.SDK_INT >=
                                Build.VERSION_CODES.Q
                            ) {

                                put(
                                    MediaStore.Video.Media.RELATIVE_PATH,
                                    Environment.DIRECTORY_MOVIES +
                                            "/VisualAttentionStudy"
                                )
                            }
                        }

                    val outputOptions =
                        MediaStoreOutputOptions.Builder(
                            context.contentResolver,
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        )
                            .setContentValues(
                                contentValues
                            )
                            .build()

                    activeCalibrationRecording =
                        videoCapture.output
                            .prepareRecording(
                                context,
                                outputOptions
                            )
                            .start(
                                mainExecutor
                            ) { event ->

                                when (event) {

                                    is VideoRecordEvent.Start -> {

                                        Log.d(
                                            "CALIBRATION_VIDEO",
                                            "Recording started"
                                        )

                                        onRecordingStarted()
                                    }
                                    is VideoRecordEvent.Status -> {

                                        val recordedDurationNs =
                                            event.recordingStats.recordedDurationNanos

                                        val recordedDurationMs =
                                            recordedDurationNs / 1_000_000L

                                        onRecordingTimeUpdate(
                                            recordedDurationMs
                                        )

                                        Log.d(
                                            "CALIBRATION_RECORDING_TIME",
                                            "RecordedDurationMs=$recordedDurationMs"
                                        )
                                    }

                                    is VideoRecordEvent.Finalize -> {

                                        activeCalibrationRecording =
                                            null

                                        if (!event.hasError()) {

                                            val location =
                                                event.outputResults
                                                    .outputUri
                                                    .toString()

                                            Log.d(
                                                "CALIBRATION_VIDEO",
                                                "VIDEO SAVED: $location"
                                            )

                                            onRecordingFinished(
                                                location
                                            )

                                        } else {

                                            val errorMessage =
                                                "Calibration video error: ${event.error}"

                                            Log.e(
                                                "CALIBRATION_VIDEO",
                                                errorMessage
                                            )

                                            onRecordingError(
                                                errorMessage
                                            )
                                        }
                                    }
                                }
                            }


                } catch (
                    e: Exception
                ) {

                    Log.e(
                        "CALIBRATION_DEBUG",
                        "Camera error: " +
                                "${e.message}"
                    )
                }
            }

        }, mainExecutor)


        // =============================================
        // CLEANUP
        // =============================================

        onDispose {

            disposed = true




            try {

                if (
                    cameraProviderFuture
                        .isDone
                ) {

                    cameraProviderFuture
                        .get()
                        .unbindAll()
                }

            } catch (
                _: Exception
            ) {
            }
        }

        }
    LaunchedEffect(
        stopRecording
    ) {

        if (stopRecording) {

            activeCalibrationRecording
                ?.stop()

            Log.d(
                "CALIBRATION_VIDEO",
                "Stop requested"
            )
        }
    }
    }
@Composable
fun CalibrationScreen(
    participantId: String,
    onBack: () -> Unit,
    onStartTest: (List<CalibrationPointData>) -> Unit
) {
    val context = LocalContext.current

    val calibrationResults = remember {
        mutableStateListOf<CalibrationPointData>()
    }

    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var calibrationStarted by remember {
        mutableStateOf(false)
    }

    var calibrationFinished by remember {
        mutableStateOf(false)
    }

    var calibrationRecordingStarted by remember {
        mutableStateOf(false)
    }

    var calibrationVideoStartTimeMs by remember {
        mutableLongStateOf(0L)
    }

    var stopCalibrationRecording by remember {
        mutableStateOf(false)
    }

    var calibrationVideoLocation by remember {
        mutableStateOf<String?>(null)
    }

    var calibrationRecordedDurationMs by remember {
        mutableLongStateOf(0L)
    }

    var calibrationRecordingError by remember {
        mutableStateOf<String?>(null)
    }

    var currentPoint by remember {
        mutableIntStateOf(0)
    }

    var calibrationDotX by remember {
        mutableStateOf(0f)
    }

    var calibrationDotY by remember {
        mutableStateOf(0f)
    }

    var calibrationDotWidth by remember {
        mutableStateOf(0f)
    }

    var calibrationDotHeight by remember {
        mutableStateOf(0f)
    }


    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            cameraPermissionGranted = granted
        }

    LaunchedEffect(Unit) {
        if (!cameraPermissionGranted) {
            permissionLauncher.launch(
                Manifest.permission.CAMERA
            )
        }
    }

    val calibrationTargets =
        studyPositions.map { position ->
            position.x to position.y
        }

    // ------------------------------------------------
    // CALIBRATION POINT DATA COLLECTION
    // ------------------------------------------------

    LaunchedEffect(
        calibrationStarted,
        calibrationRecordingStarted,
    ) {
        if (
            calibrationStarted &&
            calibrationRecordingStarted &&
            !calibrationFinished
        ) {

            while (
                currentPoint <= studyPositions.lastIndex &&
                !calibrationFinished
            ) {

                // Give the participant time to move the gaze
                // to the new calibration point.
                delay(700)

                val pointStartMs =
                    SystemClock.elapsedRealtime() -
                            calibrationVideoStartTimeMs

                // The participant looks at the calibration point
                // during this interval.
                delay(1000)

                val pointEndMs =
                    SystemClock.elapsedRealtime() -
                            calibrationVideoStartTimeMs

                val target =
                    calibrationTargets[currentPoint]

                val result =
                    CalibrationPointData(

                        positionId =
                            studyPositions[currentPoint].id,

                        targetX =
                            target.first,

                        targetY =
                            target.second,

                        targetScreenX =
                            calibrationDotX +
                                    calibrationDotWidth / 2f,

                        targetScreenY =
                            calibrationDotY +
                                    calibrationDotHeight / 2f,

                        startMs =
                            pointStartMs,

                        endMs =
                            pointEndMs
                    )

                calibrationResults.add(result)

                Log.d(
                    "CALIBRATION_DATA",
                    "Point=${currentPoint + 1} " +
                            "PositionID=${result.positionId} " +
                            "Target=(${result.targetX},${result.targetY}) " +
                            "Screen=(${result.targetScreenX},${result.targetScreenY}) " +
                            "Time=${result.startMs}-${result.endMs} ms"
                )

                if (
                    currentPoint <
                    studyPositions.lastIndex
                ) {

                    currentPoint++

                } else {

                    calibrationFinished = true
                    stopCalibrationRecording = true

                    Log.d(
                        "CALIBRATION_DEBUG",
                        "CALIBRATION FINISHED: ${calibrationResults.size} points"
                    )
                }
            }
        }
    }

    // ------------------------------------------------
    // CALIBRATION CAMERA / VIDEO
    // ------------------------------------------------

    if (calibrationStarted) {

        CalibrationFaceAnalyzer(
            participantId = participantId,
            stopRecording = stopCalibrationRecording,

            onRecordingStarted = {

                calibrationVideoStartTimeMs =
                    SystemClock.elapsedRealtime()

                calibrationRecordingStarted = true
            },

            onRecordingTimeUpdate = { recordedDurationMs ->

                calibrationRecordedDurationMs =
                    recordedDurationMs

                Log.d(
                    "CAL_RECORD_TIME",
                    "Screen received: $recordedDurationMs ms"
                )
            },

            onRecordingFinished = { location ->

                calibrationVideoLocation = location
                calibrationRecordingStarted = false


                Log.d(
                    "CALIBRATION_VIDEO",
                    "VIDEO SAVED: $location"
                )
            },

            onRecordingError = { error ->

                calibrationRecordingError = error
                calibrationRecordingStarted = false

                Log.e(
                    "CALIBRATION_VIDEO",
                    error
                )
            },

        )
    }

    // ------------------------------------------------
    // UI
    // ------------------------------------------------

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {

        // ================================================
        // BEFORE CALIBRATION
        // ================================================

        if (
            !calibrationStarted &&
            !calibrationFinished
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(
                        horizontal = 24.dp,
                        vertical = 24.dp
                    ),
                horizontalAlignment =
                    Alignment.CenterHorizontally,
                verticalArrangement =
                    Arrangement.Top
            ) {

                Spacer(
                    modifier = Modifier.height(30.dp)
                )

                Text(
                    text = "Kalibrierung",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                Text(
                    text = "Bitte sitzen Sie ruhig vor dem Smartphone und halten Sie Ihren Kopf möglichst still.",
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "Schauen Sie während der Kalibrierung jeweils auf den angezeigten Punkt.",
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(
                    modifier = Modifier.height(20.dp)
                )

                if (cameraPermissionGranted) {

                    Button(
                        onClick = {

                            currentPoint = 0

                            calibrationResults.clear()


                            calibrationFinished = false
                            stopCalibrationRecording = false
                            calibrationVideoLocation = null
                            calibrationRecordingError = null
                            calibrationRecordingStarted = false
                            calibrationRecordedDurationMs = 0L

                            calibrationStarted = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp)
                    ) {

                        Text(
                            text = "Kalibrierung starten",
                            fontSize = 18.sp
                        )
                    }

                } else {

                    Text(
                        text = "Kameraberechtigung erforderlich.",
                        textAlign = TextAlign.Center
                    )

                    Spacer(
                        modifier = Modifier.height(15.dp)
                    )

                    Button(
                        onClick = {
                            permissionLauncher.launch(
                                Manifest.permission.CAMERA
                            )
                        }
                    ) {
                        Text("Kamera erlauben")
                    }
                }

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Zurück")
                }

                Spacer(
                    modifier = Modifier.height(24.dp)
                )
            }
        }

        // ================================================
        // CALIBRATION RUNNING
        // ================================================

        if (
            calibrationStarted &&
            !calibrationFinished
        ) {

            val currentPosition =
                studyPositions[currentPoint]

            val dotSize = 105.dp

            val referenceWidth = 140.dp
            val referenceHeight = 110.dp

            val safeWidth =
                maxWidth - referenceWidth

            val safeHeight =
                maxHeight - referenceHeight

            val sharedCenterX =
                referenceWidth / 2 +
                        safeWidth * currentPosition.x

            val verticalMargin = 90.dp

            val sharedCenterY =
                referenceHeight / 2 +
                        verticalMargin +
                        (safeHeight - verticalMargin * 2) *
                        currentPosition.y

            val dotX =
                sharedCenterX - dotSize / 2

            val dotY =
                sharedCenterY - dotSize / 2

            Text(
                text = "Bitte schauen Sie auf den Punkt",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 40.dp),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            CalibrationDot(
                modifier = Modifier
                    .offset(
                        x = dotX,
                        y = dotY
                    )
                    .onGloballyPositioned { coordinates ->

                        val bounds =
                            coordinates.boundsInRoot()

                        calibrationDotX =
                            bounds.left

                        calibrationDotY =
                            bounds.top

                        calibrationDotWidth =
                            bounds.width

                        calibrationDotHeight =
                            bounds.height

                        val centerX =
                            calibrationDotX +
                                    calibrationDotWidth / 2f

                        val centerY =
                            calibrationDotY +
                                    calibrationDotHeight / 2f

                        Log.d(
                            "CAL_DOT_POSITION",
                            "Point=${currentPoint + 1} " +
                                    "PositionID=${currentPosition.id} " +
                                    "X=$calibrationDotX " +
                                    "Y=$calibrationDotY " +
                                    "Width=$calibrationDotWidth " +
                                    "Height=$calibrationDotHeight " +
                                    "CenterX=$centerX " +
                                    "CenterY=$centerY"
                        )
                    }
            )
        }

        // ================================================
        // CALIBRATION FINISHED
        // ================================================

        if (calibrationFinished) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment =
                    Alignment.CenterHorizontally,
                verticalArrangement =
                    Arrangement.Center
            ) {

                Text(
                    text = "Kalibrierung abgeschlossen",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(
                    modifier = Modifier.height(20.dp)
                )

                Text(
                    text = "Die Kalibrierung wurde erfolgreich durchgeführt.",
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(
                    modifier = Modifier.height(30.dp)
                )

                Button(
                    onClick = {
                        onStartTest(
                            calibrationResults.toList()
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                ) {
                    Text(
                        text = "Weiter",
                        fontSize = 18.sp
                    )
                }

                Spacer(
                    modifier = Modifier.height(15.dp)
                )

                OutlinedButton(
                    onClick = {

                        currentPoint = 0

                        calibrationResults.clear()


                        calibrationFinished = false
                        calibrationStarted = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Kalibrierung wiederholen"
                    )
                }

                Spacer(
                    modifier = Modifier.height(15.dp)
                )

                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Zurück")
                }
            }
        }
    }
}

@Composable
fun CalibrationDot(
    modifier: Modifier = Modifier
) {

    Surface(
        modifier = modifier
            .padding(35.dp)
            .size(35.dp),
        shape = androidx.compose.foundation.shape.CircleShape,
        color = MaterialTheme.colorScheme.primary
    ) {}
}@Composable
fun CameraPreview(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    AndroidView(
        modifier = modifier,
        factory = { ctx ->

            val previewView =
                PreviewView(ctx).apply {
                    scaleType =
                        PreviewView.ScaleType.FIT_CENTER
                }

            val cameraProviderFuture =
                ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({

                try {

                    val cameraProvider =
                        cameraProviderFuture.get()

                    val preview =
                        Preview.Builder()
                            .build()
                            .also {
                                it.setSurfaceProvider(
                                    previewView.surfaceProvider
                                )
                            }

                    cameraProvider.unbindAll()

                    cameraProvider.bindToLifecycle(
                        context as ComponentActivity,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview
                    )

                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}
fun saveCalibrationResultsToXlsx(
    context: android.content.Context,
    participantId: String,
    calibrationResults: List<CalibrationPointData>
): String {

    val timestamp = SimpleDateFormat(
        "yyyyMMdd_HHmmss",
        Locale.getDefault()
    ).format(Date())

    val safeParticipantId = participantId.replace(
        Regex("[^A-Za-z0-9_-]"),
        "_"
    )

    val fileName =
        "${safeParticipantId}_CalibrationResults_$timestamp.xlsx"

    val workbook = XSSFWorkbook()

    val sheet =
        workbook.createSheet("Calibration Results")

    // --------------------------------
    // HEADER STYLE
    // --------------------------------

    val headerStyle =
        workbook.createCellStyle()

    headerStyle.verticalAlignment =
        org.apache.poi.ss.usermodel.VerticalAlignment.CENTER

    headerStyle.alignment =
        HorizontalAlignment.CENTER

    val headerFont =
        workbook.createFont().apply {
            bold = true
        }

    headerStyle.setFont(headerFont)

    headerStyle.fillForegroundColor =
        IndexedColors.GREY_25_PERCENT.index

    headerStyle.fillPattern =
        FillPatternType.SOLID_FOREGROUND

    // --------------------------------
    // DATA STYLE
    // --------------------------------

    val dataStyle =
        workbook.createCellStyle().apply {

            alignment =
                HorizontalAlignment.CENTER

            verticalAlignment =
                org.apache.poi.ss.usermodel.VerticalAlignment.CENTER
        }

    // --------------------------------
    // HEADER
    // --------------------------------

    val headers = listOf(
        "Participant",
        "Position ID",
        "Target X",
        "Target Y",
        "Target Screen X (px)",
        "Target Screen Y (px)",
        "Start (ms)",
        "End (ms)"
    )

    val headerRow =
        sheet.createRow(0)

    headers.forEachIndexed { index, title ->

        val cell =
            headerRow.createCell(index)

        cell.setCellValue(title)
        cell.cellStyle = headerStyle
    }

    // --------------------------------
    // DATA
    // --------------------------------

    calibrationResults.forEachIndexed {
            index,
            result ->

        val row =
            sheet.createRow(index + 1)

        row.createCell(0).setCellValue(
            safeParticipantId
        )

        row.createCell(1).setCellValue(
            result.positionId.toDouble()
        )
        row.createCell(2).setCellValue(
            result.targetX.toDouble()
        )

        row.createCell(3).setCellValue(
            result.targetY.toDouble()
        )

        row.createCell(4).setCellValue(
            result.targetScreenX.toDouble()
        )

        row.createCell(5).setCellValue(
            result.targetScreenY.toDouble()
        )

        row.createCell(6).setCellValue(
            result.startMs.toDouble()
        )

        row.createCell(7).setCellValue(
            result.endMs.toDouble()
        )


        for (columnIndex in headers.indices) {
            row.getCell(columnIndex).cellStyle =
                dataStyle
        }
    }

    // --------------------------------
    // COLUMN WIDTHS
    // --------------------------------

    for (columnIndex in headers.indices) {

        sheet.setColumnWidth(
            columnIndex,
            22 * 256
        )
    }

    sheet.createFreezePane(0, 1)

    // --------------------------------
    // SAVE XLSX
    // --------------------------------

    if (
        Build.VERSION.SDK_INT >=
        Build.VERSION_CODES.Q
    ) {

        val values =
            ContentValues().apply {

                put(
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    fileName
                )

                put(
                    MediaStore.MediaColumns.MIME_TYPE,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                )

                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS +
                            "/VisualAttentionStudy"
                )
            }

        val uri =
            context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                values
            )

        if (uri != null) {

            context.contentResolver
                .openOutputStream(uri)
                ?.use { outputStream ->

                    workbook.write(
                        outputStream
                    )
                }

            workbook.close()

            return uri.toString()
        }

    } else {

        val directory =
            File(
                context.getExternalFilesDir(
                    Environment.DIRECTORY_DOCUMENTS
                ),
                "VisualAttentionStudy"
            )

        directory.mkdirs()

        val file =
            File(
                directory,
                fileName
            )

        file.outputStream().use {
                outputStream ->

            workbook.write(
                outputStream
            )
        }

        workbook.close()

        return file.absolutePath
    }

    workbook.close()

    return ""
}


fun saveImageEventsToXlsx(
    context: android.content.Context,
    participantId: String,
    events: List<ImageEvent>
): String {

    val timestamp = SimpleDateFormat(
        "yyyyMMdd_HHmmss",
        Locale.getDefault()
    ).format(Date())

    val safeParticipantId = participantId.replace(
        Regex("[^A-Za-z0-9_-]"),
        "_"
    )

    val fileName =
        "${safeParticipantId}_ImageEvents_$timestamp.xlsx"

    // --------------------------------
    // CREATE WORKBOOK
    // --------------------------------

    val workbook = XSSFWorkbook()

    val sheet =
        workbook.createSheet("Image Events")

    // --------------------------------
    // HEADER STYLE
    // --------------------------------

    val headerStyle =
        workbook.createCellStyle().apply {

            alignment =
                HorizontalAlignment.CENTER

            verticalAlignment =
                org.apache.poi.ss.usermodel
                    .VerticalAlignment.CENTER

            fillForegroundColor =
                IndexedColors.GREY_25_PERCENT.index

            fillPattern =
                FillPatternType.SOLID_FOREGROUND
        }

    val headerFont =
        workbook.createFont().apply {
            bold = true
        }

    headerStyle.setFont(headerFont)

    // --------------------------------
    // DATA STYLE
    // --------------------------------

    val dataStyle =
        workbook.createCellStyle().apply {

            alignment =
                HorizontalAlignment.CENTER

            verticalAlignment =
                org.apache.poi.ss.usermodel
                    .VerticalAlignment.CENTER
        }

    // --------------------------------
    // HEADER
    // --------------------------------
    val headers = listOf(
        "Participant",
        "Image",
        "Position ID",
        "Start (ms)",
        "End (ms)",
        "Duration (ms)",
        "X (px)",
        "Y (px)",
        "Width (px)",
        "Height (px)"
    )

    val headerRow =
        sheet.createRow(0)

    headers.forEachIndexed { index, title ->

        val cell =
            headerRow.createCell(index)

        cell.setCellValue(title)

        cell.cellStyle =
            headerStyle
    }

    // --------------------------------
    // DATA
    // --------------------------------

    events.forEachIndexed { index, event ->

        val row =
            sheet.createRow(index + 1)

        val duration =
            event.endMs - event.startMs

        row.createCell(0).setCellValue(
            participantId
        )

        row.createCell(1).setCellValue(
            event.imageName
        )

        row.createCell(2).setCellValue(
            event.positionId.toDouble()
        )

        row.createCell(3).setCellValue(
            event.startMs.toDouble()
        )

        row.createCell(4).setCellValue(
            event.endMs.toDouble()
        )

        row.createCell(5).setCellValue(
            duration.toDouble()
        )

        row.createCell(6).setCellValue(
            event.x.toDouble()
        )

        row.createCell(7).setCellValue(
            event.y.toDouble()
        )

        row.createCell(8).setCellValue(
            event.width.toDouble()
        )

        row.createCell(9).setCellValue(
            event.height.toDouble()
        )

        // Center all 10 cells
        for (columnIndex in 0..9) {

            row.getCell(columnIndex)
                .cellStyle = dataStyle
        }
    }

    // --------------------------------
    // COLUMN WIDTHS
    // --------------------------------

    sheet.setColumnWidth(
        0,
        18 * 256
    ) // Participant

    sheet.setColumnWidth(
        1,
        18 * 256
    ) // Image

    sheet.setColumnWidth(
        2,
        14 * 256
    ) // Position ID

    sheet.setColumnWidth(
        3,
        18 * 256
    ) // Start

    sheet.setColumnWidth(
        4,
        18 * 256
    ) // End

    sheet.setColumnWidth(
        5,
        18 * 256
    ) // Duration

    sheet.setColumnWidth(
        6,
        16 * 256
    ) // X

    sheet.setColumnWidth(
        7,
        16 * 256
    ) // Y

    sheet.setColumnWidth(
        8,
        16 * 256
    ) // Width

    sheet.setColumnWidth(
        9,
        16 * 256
    ) // Height

    // Keep header visible when scrolling
    sheet.createFreezePane(
        0,
        1
    )

    // --------------------------------
    // SAVE XLSX
    // --------------------------------

    if (
        Build.VERSION.SDK_INT >=
        Build.VERSION_CODES.Q
    ) {

        val values =
            ContentValues().apply {

                put(
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    fileName
                )

                put(
                    MediaStore.MediaColumns.MIME_TYPE,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                )

                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS +
                            "/VisualAttentionStudy"
                )
            }

        val uri =
            context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                values
            )

        if (uri != null) {

            context.contentResolver
                .openOutputStream(uri)
                ?.use { outputStream ->

                    workbook.write(
                        outputStream
                    )
                }

            workbook.close()

            return uri.toString()
        }

    } else {

        val directory =
            File(
                context.getExternalFilesDir(
                    Environment.DIRECTORY_DOCUMENTS
                ),
                "VisualAttentionStudy"
            )

        directory.mkdirs()

        val file =
            File(
                directory,
                fileName
            )

        file.outputStream()
            .use { outputStream ->

                workbook.write(
                    outputStream
                )
            }

        workbook.close()

        return file.absolutePath
    }

    workbook.close()

    return ""
}
@OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun BackgroundVideoRecorder(
    participantId: String,
    stopRequested: Boolean,
    onRecordingStarted: (Long) -> Unit,
    onRecordingFinished: (String) -> Unit,
    onRecordingError: (String) -> Unit
) {
    val context = LocalContext.current

    val recordingStartTime = remember {
        java.util.concurrent.atomic.AtomicLong(0L)
    }

    var activeRecording by remember {
        mutableStateOf<Recording?>(null)
    }

    DisposableEffect(participantId) {

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(context)

        val mainExecutor =
            ContextCompat.getMainExecutor(context)

        var disposed = false

        cameraProviderFuture.addListener({

            if (!disposed) {

                try {

                    val cameraProvider =
                        cameraProviderFuture.get()

                    // -----------------------------
                    // VIDEO
                    // -----------------------------

                    val recorder =
                        Recorder.Builder()
                            .setQualitySelector(
                                QualitySelector.from(Quality.HD)
                            )
                            .build()

                    val videoCapture =
                        VideoCapture.withOutput(recorder)

                    // -----------------------------
                    // BIND CAMERA
                    // -----------------------------

                    cameraProvider.unbindAll()

                    cameraProvider.bindToLifecycle(
                        context as ComponentActivity,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        videoCapture
                    )

                    // -----------------------------
                    // VIDEO FILE NAME
                    // -----------------------------

                    val safeParticipantId =
                        participantId.replace(
                            Regex("[^A-Za-z0-9_-]"),
                            "_"
                        )

                    val fileName =
                        "${safeParticipantId}_${System.currentTimeMillis()}"

                    // -----------------------------
                    // ANDROID 10+
                    // -----------------------------

                    if (
                        Build.VERSION.SDK_INT >=
                        Build.VERSION_CODES.Q
                    ) {

                        val contentValues =
                            ContentValues().apply {

                                put(
                                    MediaStore.MediaColumns.DISPLAY_NAME,
                                    fileName
                                )

                                put(
                                    MediaStore.MediaColumns.MIME_TYPE,
                                    "video/mp4"
                                )

                                put(
                                    MediaStore.Video.Media.RELATIVE_PATH,
                                    "Movies/VisualAttentionStudy"
                                )
                            }

                        val outputOptions =
                            MediaStoreOutputOptions.Builder(
                                context.contentResolver,
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            )
                                .setContentValues(
                                    contentValues
                                )
                                .build()

                        activeRecording =
                            recorder
                                .prepareRecording(
                                    context,
                                    outputOptions
                                )
                                .start(
                                    mainExecutor
                                ) { event ->

                                    when (event) {

                                        is VideoRecordEvent.Start -> {

                                            val startTime =
                                                SystemClock.elapsedRealtime()

                                            recordingStartTime.set(
                                                startTime
                                            )

                                            onRecordingStarted(
                                                startTime
                                            )
                                        }

                                        is VideoRecordEvent.Finalize -> {

                                            activeRecording = null

                                            if (event.hasError()) {

                                                onRecordingError(
                                                    "Videoaufnahme fehlgeschlagen: ${event.error}"
                                                )

                                            } else {

                                                val videoUri =
                                                    event.outputResults
                                                        .outputUri
                                                        .toString()

                                                onRecordingFinished(
                                                    videoUri
                                                )
                                            }
                                        }
                                    }
                                }

                    } else {

                        // -----------------------------
                        // ANDROID < 10
                        // -----------------------------

                        val directory =
                            File(
                                context.getExternalFilesDir(
                                    Environment.DIRECTORY_MOVIES
                                ),
                                "VisualAttentionStudy"
                            )

                        directory.mkdirs()

                        val videoFile =
                            File(
                                directory,
                                "$fileName.mp4"
                            )

                        val outputOptions =
                            FileOutputOptions.Builder(
                                videoFile
                            ).build()

                        activeRecording =
                            recorder
                                .prepareRecording(
                                    context,
                                    outputOptions
                                )
                                .start(
                                    mainExecutor
                                ) { event ->

                                    when (event) {

                                        is VideoRecordEvent.Start -> {

                                            val startTime =
                                                SystemClock.elapsedRealtime()

                                            recordingStartTime.set(
                                                startTime
                                            )

                                            onRecordingStarted(
                                                startTime
                                            )
                                        }

                                        is VideoRecordEvent.Finalize -> {

                                            activeRecording = null

                                            if (event.hasError()) {

                                                onRecordingError(
                                                    "Videoaufnahme fehlgeschlagen: ${event.error}"
                                                )

                                            } else {

                                                onRecordingFinished(
                                                    videoFile.absolutePath
                                                )
                                            }
                                        }
                                    }
                                }
                    }

                } catch (e: Exception) {

                    onRecordingError(
                        e.message
                            ?: "Unbekannter Kamerafehler"
                    )
                }
            }

        }, mainExecutor)

        onDispose {

            disposed = true

            activeRecording?.stop()
            activeRecording = null

            try {

                if (cameraProviderFuture.isDone) {

                    cameraProviderFuture
                        .get()
                        .unbindAll()
                }

            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(stopRequested) {

        if (stopRequested) {

            activeRecording?.stop()
        }
    }
}

@Composable
fun TestScreen(
    participantId: String,
    onTestFinished: (List<Int>) -> Unit
) {
    val context = LocalContext.current

    val shownImages = remember {
        mutableStateListOf<Int>()
    }

    val imageEvents = remember {
        mutableStateListOf<ImageEvent>()
    }
    val selectedImages = remember {
        studyImages
            .shuffled()
            .take(15)
    }
    val randomizedPositions = remember {
        studyPositions.shuffled()
    }

    val imageShowTimes = listOf(
        10,
        20,
        30,
        40,
        50,
        60,
        70,
        80,
        90,
        100,
        110,
        120,
        130,
        140,
        150
    )

    val testDurationSeconds = 180

    var secondsLeft by remember {
        mutableIntStateOf(testDurationSeconds)
    }

    var testStartTime by remember {
        mutableLongStateOf(0L)
    }

    var currentDistractorStartMs by remember {
        mutableLongStateOf(0L)
    }
    var currentDistractorX by remember {
        mutableStateOf(0f)
    }

    var currentDistractorY by remember {
        mutableStateOf(0f)
    }

    var currentDistractorWidth by remember {
        mutableStateOf(0f)
    }

    var currentDistractorHeight by remember {
        mutableStateOf(0f)
    }

    var currentDistractor by remember {
        mutableStateOf<Int?>(null)
    }
    var currentStudyImage by remember {
        mutableStateOf<StudyImage?>(null)
    }
    var currentDistractorAlignment by remember {
        mutableStateOf<Alignment>(
            Alignment.Center
        )
    }
    var currentDistractorPositionId by remember {
        mutableIntStateOf(0)
    }
    var currentDistractorPosition by remember {
        mutableStateOf<StudyPosition?>(null)
    }


    var recordingStarted by remember {
        mutableStateOf(false)
    }

    var stopRecording by remember {
        mutableStateOf(false)
    }

    var recordingError by remember {
        mutableStateOf<String?>(null)
    }

    BackgroundVideoRecorder(
        participantId = participantId,
        stopRequested = stopRecording,




        onRecordingStarted = { startTime ->

            testStartTime = startTime
            recordingStarted = true
        },

        onRecordingFinished = { videoLocation ->

            println(
                "VIDEO SAVED: $videoLocation"
            )


            Log.d(
                "ATTENTION_DEBUG",
                "IMAGE EVENTS COUNT: ${imageEvents.size}"
            )

            imageEvents.forEach { event ->

                Log.d(
                    "ATTENTION_DEBUG",
                    "EVENT ${event.imageName}: " +
                            "${event.startMs}-${event.endMs}"
                )
            }
            // --------------------------------
            // SAVE IMAGE EVENTS XLSX
            // --------------------------------

            val eventsSnapshot =
                imageEvents.toList()
            val eventsPath =
                saveImageEventsToXlsx(
                    context = context,
                    participantId = participantId,
                    events = eventsSnapshot
                )

            Log.d(
                "ATTENTION_DEBUG",
                "IMAGE EVENTS XLSX SAVED: $eventsPath"
            )

            // --------------------------------
            // GO TO QUIZ
            // --------------------------------
            onTestFinished(
                shownImages.toList(),
            )
        },

        onRecordingError = { error ->

            Log.e(
                "ATTENTION_DEBUG",
                "RECORDING ERROR: $error"
            )

            recordingError = error
        }
    )

    LaunchedEffect(recordingStarted) {

        if (recordingStarted) {

            while (secondsLeft > 0) {

                kotlinx.coroutines.delay(1000)

                secondsLeft--

                val elapsedSeconds =
                    testDurationSeconds -
                            secondsLeft

                val nowMs =
                    SystemClock.elapsedRealtime() -
                            testStartTime

                selectedImages.forEachIndexed { index,
                                                studyImage ->

                    val showTime =
                        imageShowTimes[index]

                    val hideTime =
                        showTime + 2

                    if (elapsedSeconds == showTime) {

                        currentStudyImage =
                            studyImage

                        currentDistractor =
                            studyImage.imageRes
                        val selectedPosition =
                            randomizedPositions[index]

                        currentDistractorPosition =
                            selectedPosition

                        currentDistractorPositionId =
                            selectedPosition.id

                        currentDistractorStartMs =
                            nowMs

                        if (
                            !shownImages.contains(
                                studyImage.imageRes
                            )
                        ) {

                            shownImages.add(
                                studyImage.imageRes
                            )
                        }

                        Log.d(
                            "ATTENTION_DEBUG",
                            "SHOW ${studyImage.imageName} at $nowMs ms"
                        )
                    }

                    if (
                        elapsedSeconds == hideTime &&
                        currentStudyImage?.imageRes ==
                        studyImage.imageRes
                    ) {
                        imageEvents.add(
                            ImageEvent(
                                imageName =
                                    studyImage.imageName,

                                startMs =
                                    currentDistractorStartMs,

                                endMs =
                                    nowMs,

                                positionId =
                                    currentDistractorPositionId,

                                x =
                                    currentDistractorX,

                                y =
                                    currentDistractorY,

                                width =
                                    currentDistractorWidth,

                                height =
                                    currentDistractorHeight
                            )
                        )

                        Log.d(
                            "ATTENTION_DEBUG",
                            "HIDE ${studyImage.imageName} at $nowMs ms"
                        )

                        currentDistractor = null
                        currentStudyImage = null
                    }
                }
            }

            currentDistractor = null
            currentStudyImage = null

            stopRecording = true
        }
    }

    if (recordingError != null) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(30.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {

            Text(
                text = "Fehler bei der Videoaufnahme",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Text(
                text = recordingError ?: ""
            )
        }

        return
    }

    if (!recordingStarted) {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                CircularProgressIndicator()

                Spacer(
                    modifier = Modifier.height(20.dp)
                )

                Text(
                    text = "Kamera wird vorbereitet...",
                    fontSize = 18.sp
                )
            }
        }

        return
    }

    val infiniteTransition =
        rememberInfiniteTransition(
            label = "TargetMovement"
        )

    val horizontalProgress by
    infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = 4000,
                        easing = LinearEasing
                    ),
                repeatMode =
                    RepeatMode.Reverse
            ),
        label = "HorizontalMovement"
    )

    val verticalProgress by
    infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = 5500,
                        easing = LinearEasing
                    ),
                repeatMode =
                    RepeatMode.Reverse
            ),
        label = "VerticalMovement"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
    ) {

        val ballSize = 45.dp

        val availableWidth =
            maxWidth - ballSize

        val availableHeight =
            maxHeight - ballSize

        val xPosition =
            availableWidth *
                    horizontalProgress

        val yPosition =
            availableHeight *
                    verticalProgress

        Text(
            text = String.format(
                "%02d:%02d",
                secondsLeft / 60,
                secondsLeft % 60
            ),
            modifier =
                Modifier.align(
                    Alignment.TopCenter
                ),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Surface(
            modifier = Modifier
                .offset(
                    x = xPosition,
                    y = yPosition
                )
                .size(ballSize),
            shape = CircleShape,
            color =
                MaterialTheme.colorScheme.primary
        ) {}
        val distractorWidth = 140.dp
        val distractorHeight = 110.dp

        val distractorAvailableWidth =
            maxWidth - distractorWidth

        val distractorAvailableHeight =
            maxHeight - distractorHeight

        currentDistractor?.let { distractorId ->
            val position =
                currentDistractorPosition

            if (position != null) {

                val sharedCenterX =
                    distractorWidth / 2 +
                            distractorAvailableWidth * position.x

                val verticalMargin = 90.dp

                val sharedCenterY =
                    distractorHeight / 2 +
                            verticalMargin +
                            (distractorAvailableHeight - verticalMargin * 2) *
                            position.y
                val distractorX =
                    sharedCenterX - distractorWidth / 2

                val distractorY =
                    sharedCenterY - distractorHeight / 2

                Card(
                    modifier = Modifier
                        .offset(
                            x = distractorX,
                            y = distractorY
                        )
                        .size(
                            width = distractorWidth,
                            height = distractorHeight
                        )
                        .onGloballyPositioned { coordinates ->

                            val bounds =
                                coordinates.boundsInRoot()

                            currentDistractorX =
                                bounds.left

                            currentDistractorY =
                                bounds.top

                            currentDistractorWidth =
                                bounds.width

                            currentDistractorHeight =
                                bounds.height

                            Log.d(
                                "DISTRACTOR_POSITION",
                                "PositionID=$currentDistractorPositionId " +
                                        "X=$currentDistractorX " +
                                        "Y=$currentDistractorY " +
                                        "Width=$currentDistractorWidth " +
                                        "Height=$currentDistractorHeight"
                            )
                        }
                ) {


                    Box(
                        modifier =
                            Modifier.fillMaxSize(),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Image(
                            painter =
                                painterResource(
                                    id = distractorId
                                ),
                            contentDescription =
                                "Distraktor",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                            contentScale =
                                ContentScale.Fit
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun QuizScreen(
    participantId: String,
    age: String,
    gender: String,
    shownImages: Set<Int>,
    onFinish: () -> Unit
) {

    val context = LocalContext.current
    data class QuizQuestion(
        val imageRes: Int,
        val imageName: String,
        val correctAnswer: Boolean
    )

    val questions = remember(shownImages) {

        studyImages
            .map { studyImage ->

                QuizQuestion(
                    imageRes =
                        studyImage.imageRes,

                    imageName =
                        studyImage.imageName,

                    correctAnswer =
                        shownImages.contains(
                            studyImage.imageRes
                        )
                )
            }
            .shuffled()
    }

    var currentQuestion by remember {
        mutableIntStateOf(0)
    }

    var correctAnswers by remember {
        mutableIntStateOf(0)
    }

    var quizFinished by remember {
        mutableStateOf(false)
    }

    var csvSaved by remember {
        mutableStateOf(false)
    }

    var csvLocation by remember {
        mutableStateOf("")
    }

    val userAnswers =
        remember {
            mutableStateListOf<QuizAnswer>()
        }

    fun answerQuestion(
        userAnswer: Boolean
    ) {

        val question =
            questions[currentQuestion]

        val isCorrect =
            userAnswer ==
                    question.correctAnswer

        if (isCorrect) {
            correctAnswers++
        }

        userAnswers.add(

            QuizAnswer(
                questionNumber =
                    currentQuestion + 1,

                imageName =
                    question.imageName,

                shownDuringTest =
                    question.correctAnswer,

                userAnswer =
                    userAnswer,

                correct =
                    isCorrect
            )
        )

        if (
            currentQuestion <
            questions.lastIndex
        ) {

            currentQuestion++

        } else {

            quizFinished = true
        }
    }

    LaunchedEffect(quizFinished) {

        if (
            quizFinished &&
            !csvSaved
        ) {


            csvLocation =
                saveQuizResultsToXlsx(
                    context = context,
                    participantId =
                        participantId,
                    age = age,
                    gender = gender,
                    answers =
                        userAnswers.toList()
                )

            csvSaved = true
        }
    }

    if (!quizFinished) {

        val question =
            questions[currentQuestion]

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally,

            verticalArrangement =
                Arrangement.Center
        ) {

            Text(
                text = "Erinnerungstest",
                fontSize = 28.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(15.dp)
            )

            Text(
                text =
                    "Frage ${currentQuestion + 1} von ${questions.size}",
                fontSize = 17.sp
            )

            Spacer(
                modifier =
                    Modifier.height(30.dp)
            )

            Card(
                modifier =
                    Modifier.size(
                        width = 240.dp,
                        height = 200.dp
                    )
            ) {

                Image(
                    painter =
                        painterResource(
                            id =
                                question.imageRes
                        ),

                    contentDescription =
                        question.imageName,

                    modifier = Modifier
                        .fillMaxSize()
                        .padding(15.dp),

                    contentScale =
                        ContentScale.Fit
                )
            }

            Spacer(
                modifier =
                    Modifier.height(30.dp)
            )

            Text(
                text =
                    "Haben Sie dieses Bild während des Tests gesehen?",
                fontSize = 20.sp,
                fontWeight =
                    FontWeight.Medium
            )

            Spacer(
                modifier =
                    Modifier.height(35.dp)
            )

            Button(
                onClick = {
                    answerQuestion(true)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
            ) {

                Text(
                    text = "Ja",
                    fontSize = 18.sp
                )
            }

            Spacer(
                modifier =
                    Modifier.height(15.dp)
            )

            OutlinedButton(
                onClick = {
                    answerQuestion(false)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
            ) {

                Text(
                    text = "Nein",
                    fontSize = 18.sp
                )
            }
        }

    } else {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally,

            verticalArrangement =
                Arrangement.Center
        ) {

            Text(
                text =
                    "Quiz abgeschlossen",
                fontSize = 28.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(25.dp)
            )

            Text(
                text =
                    "Teilnehmer: $participantId",
                fontSize = 18.sp
            )

            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )

            Text(
                text =
                    "$correctAnswers von ${questions.size} Antworten richtig",
                fontSize = 24.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )

            if (csvSaved) {

                Text(
                    text =
                        "Ergebnisse wurden gespeichert.",
                    fontSize = 16.sp
                )
            }

            Spacer(
                modifier =
                    Modifier.height(35.dp)
            )

            Button(
                onClick = onFinish,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
            ) {

                Text(
                    text =
                        "Ergebnis anzeigen",
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
fun ResultScreen(
    participantId: String
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {


        Text(
            text = "Studie abgeschlossen",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Teilnehmer-ID: $participantId",
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Die Videoaufnahme und der Erinnerungstest wurden abgeschlossen.",
            fontSize = 18.sp
        )
    }
}