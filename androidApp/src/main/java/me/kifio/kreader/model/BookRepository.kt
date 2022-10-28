/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package me.kifio.kreader.model

import me.kifio.kreader.model.db.BooksDao

class BookRepository(private val booksDao: BooksDao) {

    fun books() = booksDao.getAllBooks()

    fun get(id: Long) = booksDao.get(id)
//
//    suspend fun insertBook(href: String, mediaType: MediaType, publication: Publication): Long {
//        val book = Book(
//            creation = DateTime().toDate().time,
//            title = publication.metadata.title,
//            author = publication.metadata.authorName,
//            href = href,
//            identifier = publication.metadata.identifier ?: "",
//            type = mediaType.toString(),
//            progression = "{}"
//        )
//        return booksDao.insertBook(book)
//    }
//
//    suspend fun deleteBook(id: Long) = booksDao.deleteBook(id)
//
//    suspend fun saveProgression(locator: Locator, bookId: Long) =
//        booksDao.saveProgression(locator.toJSON().toString(), bookId)
//
//    suspend fun insertBookmark(bookId: Long, publication: Publication, locator: Locator): Long {
//        val resource = publication.readingOrder.indexOfFirstWithHref(locator.href)!!
//        val bookmark = Bookmark(
//            creation = DateTime().toDate().time,
//            bookId = bookId,
//            publicationId = publication.metadata.identifier ?: publication.metadata.title,
//            resourceIndex = resource.toLong(),
//            resourceHref = locator.href,
//            resourceType = locator.type,
//            resourceTitle = locator.title.orEmpty(),
//            location = locator.locations.toJSON().toString(),
//            locatorText = Locator.Text().toJSON().toString()
//        )
//
//        return booksDao.insertBookmark(bookmark)
//    }
//
//    fun bookmarksForBook(bookId: Long): LiveData<MutableList<Bookmark>> =
//        booksDao.getBookmarksForBook(bookId)
//
//    suspend fun deleteBookmark(bookmarkId: Long) = booksDao.deleteBookmark(bookmarkId)
//
//    suspend fun highlightById(id: Long): Highlight? =
//        booksDao.getHighlightById(id)
//
//    fun highlightsForBook(bookId: Long): Flow<List<Highlight>> =
//        booksDao.getHighlightsForBook(bookId)
//
//    suspend fun addHighlight(bookId: Long, style: Highlight.Style, @ColorInt tint: Int, locator: Locator, annotation: String): Long =
//        booksDao.insertHighlight(Highlight(bookId, style, tint, locator, annotation))
//
//    suspend fun deleteHighlight(id: Long) = booksDao.deleteHighlight(id)
//
//    suspend fun updateHighlightAnnotation(id: Long, annotation: String) {
//        booksDao.updateHighlightAnnotation(id, annotation)
//    }
//
//    suspend fun updateHighlightStyle(id: Long, style: Highlight.Style, @ColorInt tint: Int) {
//        booksDao.updateHighlightStyle(id, style, tint)
//    }
}