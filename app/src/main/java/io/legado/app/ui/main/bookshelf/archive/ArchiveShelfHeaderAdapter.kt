package io.legado.app.ui.main.bookshelf.archive

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.data.entities.Book
import io.legado.app.databinding.ItemArchiveShelfHeaderBinding

/**
 * 档案式书架的头部：上方立体书架，下方 SHELF / CATALOGUE 标题、收起/展开与排序入口。
 */
class ArchiveShelfHeaderAdapter(
    private val onOpen: (Book) -> Unit,
    private val onInfo: (Book, ArchiveShelfView?) -> Unit
) : RecyclerView.Adapter<ArchiveShelfHeaderAdapter.Holder>() {

    private var books: List<Book> = emptyList()
    private var visibleCount = ArchiveShelfGeometry.DEFAULT_VISIBLE_COUNT
    private var shelfVisible = true

    /** 当前排序方式，显示在排序按钮上 */
    var sortLabel: String = ""

    var onSortClick: (() -> Unit)? = null

    class Holder(val binding: ItemArchiveShelfHeaderBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            ItemArchiveShelfHeaderBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun getItemCount(): Int = 1

    fun submit(list: List<Book>, count: Int) {
        books = list
        visibleCount = count
        notifyItemChanged(0)
    }

    fun toggleShelf() {
        shelfVisible = !shelfVisible
        notifyItemChanged(0)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.binding.run {
            val showShelf = shelfVisible && books.isNotEmpty()
            archiveShelf.visibility = if (showShelf) View.VISIBLE else View.GONE
            if (showShelf) {
                archiveShelf.onBookClick = onOpen
                // 详情页的封面飞行要从书架本体取几何，这里把视图一起带出去
                archiveShelf.onBookLongClick = { book -> onInfo(book, archiveShelf) }
                archiveShelf.setBooks(books, visibleCount)
            }
            btnToggleShelf.visibility = if (books.isEmpty()) View.GONE else View.VISIBLE
            btnToggleShelf.setText(
                if (shelfVisible) R.string.archive_shelf_collapse
                else R.string.archive_shelf_expand
            )
            btnToggleShelf.setOnClickListener { toggleShelf() }
            btnSort.text = sortLabel.ifBlank { btnSort.context.getString(R.string.sort) }
            btnSort.setOnClickListener { onSortClick?.invoke() }
        }
    }
}
