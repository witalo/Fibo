package com.example.fibo.ui.screens.quotation
import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import java.io.File

@Composable
fun PdfViewerAndroidView(
    file: File,
    modifier: Modifier = Modifier,
    onPageChange: (Int) -> Unit = {},
    onPdfLoadComplete: () -> Unit = {}
) {
    val pdfViewerRef = remember { mutableListOf<PDFView?>() }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            // Create a LinearLayout to hold the PDFView
            val layout = LinearLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            // Create and add the PDFView to the layout
            val pdfView = createPdfView(context, layout, file, onPageChange, onPdfLoadComplete)
            pdfViewerRef.add(pdfView)

            layout
        },
        update = { layout ->
            // Update PDFView when the file changes
            val pdfView = pdfViewerRef.firstOrNull()

            if (pdfView != null && pdfView.parent == layout) {
                // PDFView is already added to the layout, just update it
                updatePdfView(pdfView, file, onPageChange, onPdfLoadComplete)
            } else {
                // Clear the layout and add a new PDFView
                layout.removeAllViews()
                val newPdfView = createPdfView(layout.context, layout, file, onPageChange, onPdfLoadComplete)
                pdfViewerRef.clear()
                pdfViewerRef.add(newPdfView)
            }
        }
    )
}

/**
 * Create a new PDFView and add it to the parent layout
 */
private fun createPdfView(
    context: Context,
    parent: ViewGroup,
    file: File,
    onPageChange: (Int) -> Unit,
    onPdfLoadComplete: () -> Unit
): PDFView {
    // Create a new PDFView with layout parameters
    val pdfView = PDFView(context, null).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
    }

    // Add the PDFView to the parent layout
    parent.addView(pdfView)

    // Load the PDF file
    updatePdfView(pdfView, file, onPageChange, onPdfLoadComplete)

    return pdfView
}

/**
 * Update the PDFView with a new file
 */
private fun updatePdfView(
    pdfView: PDFView,
    file: File,
    onPageChange: (Int) -> Unit,
    onPdfLoadComplete: () -> Unit
) {
    pdfView.fromFile(file)
        .enableSwipe(true)
        .swipeHorizontal(false)
        .enableDoubletap(true)
        .defaultPage(0)
        .enableAnnotationRendering(true)
        .scrollHandle(DefaultScrollHandle(pdfView.context))
        .spacing(10)
        .onPageChange { page, _ -> onPageChange(page) }
        .onLoad { onPdfLoadComplete() }
        .load()
}