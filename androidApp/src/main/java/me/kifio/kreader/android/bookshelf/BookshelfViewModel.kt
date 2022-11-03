package me.kifio.kreader.android.bookshelf

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.kifio.kreader.android.Application
import me.kifio.kreader.android.db.BookDatabase
import me.kifio.kreader.android.model.Book
import me.kifio.kreader.android.reader.ReaderActivityContract
import me.kifio.kreader.android.reader.ReaderInitData
import me.kifio.kreader.android.reader.ReaderRepository
import me.kifio.kreader.android.reader.VisualReaderInitData
import me.kifio.kreader.android.utils.extensions.copyToLocalFile
import me.kifio.kreader.android.utils.extensions.screenWidth
import org.json.JSONObject
import org.readium.r2.lcp.LcpService
import org.readium.r2.shared.Injectable
import org.readium.r2.shared.UserException
import org.readium.r2.shared.extensions.mediaType
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.asset.FileAsset
import org.readium.r2.shared.publication.services.coverFitting
import org.readium.r2.shared.publication.services.isRestricted
import org.readium.r2.shared.publication.services.protectionError
import org.readium.r2.shared.util.Try
import org.readium.r2.streamer.Streamer
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL

sealed class BookShelfError {
    object BookNotAddedError : BookShelfError()
    object FileNotCreatedError : BookShelfError()
    object BookAlreadyOpenedError : BookShelfError()
    object FileNotFoundError : BookShelfError()
    object PublicationOpeningError : BookShelfError()
}

class BookshelfViewModel : ViewModel() {

    var shelfState by mutableStateOf<List<Book>?>(null)
        private set

    var errorsState by mutableStateOf<BookShelfError?>(null)
        private set

    private var booksRepository: BookRepository? = null
    private var streamer: Streamer? = null

    fun setup(ctx: Context) {
        viewModelScope.launch(context = Dispatchers.IO) {
            booksRepository = (ctx.applicationContext as Application).bookRepository
            streamer = Streamer(ctx, contentProtections = emptyList())
            loadBooks()
        }
    }

    fun loadBooks() {
        viewModelScope.launch(context = Dispatchers.IO) {
            val books = booksRepository?.books()
            withContext(context = Dispatchers.Main) {
                shelfState = when (books == null) {
                    true -> null
                    false -> books
                }
            }
        }
    }

    fun saveBookToLocalStorage(ctx: Context, uri: Uri) {
        viewModelScope.launch(context = Dispatchers.Default) {
            when (val localFile = uri.copyToLocalFile(ctx, ctx.filesDir.absolutePath)) {
                null -> errorsState = BookShelfError.FileNotCreatedError
                else -> importPublication(ctx = ctx, localFile = localFile)
            }
            loadBooks()
        }
    }

    private suspend fun importPublication(ctx: Context, localFile: File) {
        val booksRepo = this.booksRepository ?: throw java.lang.IllegalStateException()
        val libraryAsset = FileAsset(localFile, localFile.mediaType())

        streamer?.open(libraryAsset, allowUserInteraction = false)
            ?.onSuccess { publication ->
                try {
                    val id = booksRepo.insertBook(
                        libraryAsset.file.path,
                        localFile.mediaType(),
                        publication
                    )
                    val coverPath = storeCoverImage(ctx, id, publication)
                } catch (e: java.lang.IllegalArgumentException) {
                    e.printStackTrace()
                    errorsState = BookShelfError.BookNotAddedError
                }
            }
            ?.onFailure { e ->
                tryOrNull { localFile.delete() }
            }
    }

    private suspend fun storeCoverImage(
        ctx: Context,
        bookId: Long,
        publication: Publication
    ): String? =
        withContext(Dispatchers.IO) {
            val coverImageDir = File("${ctx.filesDir}covers/")
            if (!coverImageDir.exists()) {
                coverImageDir.mkdirs()
            }

            val coverImageFile = File("${ctx.filesDir}covers/${bookId}.png")

            val bitmap: Bitmap = publication.coverFitting(
                Size(ctx.screenWidth / 2, ctx.screenWidth / 2)
            ) ?: return@withContext null

            var fos: FileOutputStream? = null

            try {
                fos = FileOutputStream(coverImageFile)
                bitmap.compress(Bitmap.CompressFormat.PNG, 80, fos)
                return@withContext coverImageFile.absolutePath
            } catch (e: IOException) {
                e.printStackTrace()
                return@withContext null
            } finally {
                fos?.flush()
                fos?.close()
            }
        }

    fun openBook(
        ctx: Context,
        book: Book,
        onBookOpened: (Long) -> Unit,
    ) = viewModelScope.launch(Dispatchers.IO) {
        val app = ctx.applicationContext as Application
        val readerRepository = app.readerRepository.await()
        readerRepository.open(book.id, ctx)
            .onFailure { exception ->
                if (exception is ReaderRepository.CancellationException)
                    return@launch

                val message = when (exception) {
                    is UserException -> exception.getUserMessage(ctx)
                    else -> exception.message
                }
                withContext(Dispatchers.Main) {
                    errorsState = BookShelfError.PublicationOpeningError
                }
            }
            .onSuccess {
                onBookOpened(book.id)
            }
    }

    fun closeBook(ctx: Context, bookId: Long) = viewModelScope.launch {
        val readerRepository = (ctx.applicationContext as Application).readerRepository.await()
        readerRepository.close(bookId)
    }
}