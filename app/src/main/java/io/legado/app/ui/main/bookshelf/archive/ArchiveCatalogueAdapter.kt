package io.legado.app.ui.main.bookshelf.archive

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.data.entities.Book
import io.legado.app.databinding.ItemArchiveCatalogueBinding

/**
 * 档案式书目的目录行：FILE / 编号 + 封面 + 书名 + 作者 + 阅读进度。
 * 单击打开阅读，长按打开书籍详情，和原书架的行为保持一致。
 */
class ArchiveCatalogueAdapter(
    private val fragment: Fragment,
    private val lifecycle: Lifecycle,
    private val onOpen: (Book) -> Unit,
    private val onInfo: (Book) -> Unit
) : ListAdapter<Book, ArchiveCatalogueAdapter.Holder>(Diff) {

    class Holder(val binding: ItemArchiveCatalogueBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            ItemArchiveCatalogueBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val book = getItem(position)
        holder.binding.run {
            tvCode.text = "档案 / %03d".format(position + 1)
            tvName.text = book.name
            tvAuthor.text = book.author
            tvProgress.text = book.durChapterTitle.orEmpty()
            ivCover.loadThumb(book, false, fragment, lifecycle)
            rowContent.setOnClickListener { onOpen(book) }
            rowContent.setOnLongClickListener {
                onInfo(book)
                true
            }
        }
    }

    private object Diff : DiffUtil.ItemCallback<Book>() {
        override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean {
            return oldItem.bookUrl == newItem.bookUrl
        }

        override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean {
            return oldItem.name == newItem.name &&
                oldItem.author == newItem.author &&
                oldItem.durChapterTitle == newItem.durChapterTitle &&
                oldItem.getDisplayCover() == newItem.getDisplayCover()
        }
    }
}
