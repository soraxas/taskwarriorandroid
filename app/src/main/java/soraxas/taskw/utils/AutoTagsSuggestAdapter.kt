package soraxas.taskw.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import java.util.*

class AutoTagsSuggestAdapter(context: Context, private val resource: Int, private val items: List<String>)
    : ArrayAdapter<Any?>(context, resource, 0, items) {
    private val tempItems: MutableList<String> = ArrayList(items)
    private val suggestions: MutableList<String> = ArrayList()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = items[position]
        val view: View = convertView ?: run {
            val inflater = context.getSystemService(Context
                    .LAYOUT_INFLATER_SERVICE) as LayoutInflater
            inflater.inflate(resource, parent, false)
        }
        if (view is TextView) {
            view.text = item
        }
        return view
    }

    override fun getFilter(): Filter {
        return nameFilter
    }

    private var nameFilter: Filter = object : Filter() {
        override fun convertResultToString(resultValue: Any): CharSequence {
            return resultValue as String
        }

        override fun performFiltering(constraint: CharSequence): FilterResults {
            return run {
                suggestions.clear()
                val constraintStr = constraint.toString()
                val lastCharIsSpace = constraintStr.isNotEmpty() &&
                        constraintStr[constraintStr.length - 1] == ' '
                val tags = listOf(*constraintStr.split(" ").toTypedArray())
                // if the last char is space, we will treat constraint as if it's a wildcard
                val prefix = if (lastCharIsSpace) {
                    constraintStr
                } else {
                    val sb = StringBuilder()
                    for (i in 0 until tags.size - 1) {
                        sb.append(tags[i])
                        sb.append(" ")
                    }
                    sb.toString()
                }

                // the actual term to search for
                val constraintTerm = tags[tags.size - 1].toLowerCase(Locale.ROOT)
                tempItems.sort()
                for (names in tempItems) {
                    //                    if (names.toLowerCase().contains(last_terms))
                    if (lastCharIsSpace || names.toLowerCase(Locale.ROOT).startsWith(constraintTerm)) {
                        // the term shouldn't already appear previously
                        if (!tags.contains(names)) {
                            suggestions.add(String.format("%s %s", prefix, names))
                        }
                    }
                }
                val filterResults = FilterResults()
                filterResults.values = suggestions
                filterResults.count = suggestions.size
                filterResults
            }
        }

        override fun publishResults(constraint: CharSequence, results: FilterResults) {
            val filterList: List<String> = results.values as ArrayList<String>
            if (results.count > 0) {
                clear()
                for (item in filterList) {
                    add(item)
                    notifyDataSetChanged()
                }
            }
        }
    }
}