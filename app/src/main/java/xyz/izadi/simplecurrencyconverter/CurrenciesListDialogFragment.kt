package xyz.izadi.simplecurrencyconverter

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_currencies_list_dialog.*
import kotlinx.android.synthetic.main.fragment_currencies_list_dialog_item.view.*
import xyz.izadi.simplecurrencyconverter.data.api.Currencies
import java.util.*


const val ARG_CURRENCIES = "currencies_object"

class CurrenciesListDialogFragment : BottomSheetDialogFragment() {

    private var mListener: Listener? = null
    private var mAdapter: CurrenciesAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_currencies_list_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val layoutManager = LinearLayoutManager(context)
        rv_currency_list.layoutManager = layoutManager

        val currencies: Currencies = requireArguments().getParcelable(ARG_CURRENCIES)!!
        currencies.totalCurrencies = currencies.currencies.size

        mAdapter = CurrenciesAdapter(currencies)
        rv_currency_list.adapter = mAdapter

        setUpQueryListener()
        setUpScrollListener()
    }

    private fun setUpScrollListener() {
        rv_currency_list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0) {
                    val imm =
                        context?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                    //Find the currently focused view, so we can grab the correct window token from it.
                    val view = view?.rootView?.windowToken
                    imm.hideSoftInputFromWindow(view, 0)
                }
            }
        })
    }

    private fun setUpQueryListener() {
        sv_currencies.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                val parentWithBSBehavior = cl_bs_currency_selector.parent as FrameLayout
                val mBottomSheetBehavior = BottomSheetBehavior.from(parentWithBSBehavior)

                mBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

                mAdapter?.filter?.filter(query)
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val parentWithBSBehavior = cl_bs_currency_selector.parent as FrameLayout
                val mBottomSheetBehavior = BottomSheetBehavior.from(parentWithBSBehavior)

                mBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

                mAdapter?.filter?.filter(newText)
                return false
            }
        })
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val parent = parentFragment
        mListener = if (parent != null) {
            parent as Listener
        } else {
            context as Listener
        }
    }

    override fun onDetach() {
        mListener = null
        super.onDetach()
    }

    interface Listener {
        fun onCurrencyClicked(code: String)
    }

    private inner class ViewHolder internal constructor(
        inflater: LayoutInflater,
        parent: ViewGroup
    ) : RecyclerView.ViewHolder(
        inflater.inflate(
            R.layout.fragment_currencies_list_dialog_item,
            parent,
            false
        )
    ) {

        internal val cod: TextView = itemView.tv_currency_code
        internal val desc: TextView = itemView.tv_currency_desc

        init {
            itemView.setOnClickListener {
                mListener?.let {
                    it.onCurrencyClicked(cod.text.toString())
                    dismiss()
                }
            }
        }
    }

    private inner class CurrenciesAdapter internal constructor(
        private val mCurrencies: Currencies,
        private var mCurrenciesFiltered: Currencies = mCurrencies
    ) : RecyclerView.Adapter<ViewHolder>(), Filterable {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(parent.context), parent)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (position < mCurrenciesFiltered.currencies.size) {
                mCurrenciesFiltered.currencies
                val currency = mCurrenciesFiltered.currencies.keys.toList()[position]
                holder.cod.text = currency
                holder.desc.text = getString(R.string.currency_desc, mCurrencies.currencies[currency])
            }
        }

        override fun getItemCount(): Int {
            return mCurrenciesFiltered.totalCurrencies
        }

        override fun getFilter(): Filter {
            return object : Filter() {
                override fun performFiltering(charSequence: CharSequence): FilterResults {
                    val charString = charSequence.toString().toLowerCase(Locale.ROOT)

                    mCurrenciesFiltered = if (charString.isEmpty()) {
                        mCurrencies
                    } else {
                        val filteredList = mutableMapOf<String, String>()
                        for ((code, name) in mCurrencies.currencies) {
                            if (code.toLowerCase(Locale.ROOT).contains(charString)
                                || name.toLowerCase(Locale.ROOT).contains(charString)
                            ) {
                                filteredList[code] = name
                            }
                        }
                        Currencies(true, filteredList.size, filteredList)
                    }

                    val filterResults = FilterResults()
                    filterResults.values = mCurrenciesFiltered

                    return filterResults
                }

                override fun publishResults(
                    charSequence: CharSequence,
                    filterResults: FilterResults
                ) {
                    mCurrenciesFiltered = filterResults.values as Currencies

                    // refresh the list with filtered data
                    notifyDataSetChanged()
                }
            }
        }
    }

    companion object {
        fun newInstance(currencies: Currencies): CurrenciesListDialogFragment =
            CurrenciesListDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_CURRENCIES, currencies)
                }
            }

    }
}
