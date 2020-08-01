package xyz.izadi.simplecurrencyconverter

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.activity_main.*
import xyz.izadi.simplecurrencyconverter.data.CurrenciesViewModel
import xyz.izadi.simplecurrencyconverter.data.api.Currencies
import xyz.izadi.simplecurrencyconverter.data.api.Rates
import xyz.izadi.simplecurrencyconverter.data.reformatIfNeeded
import java.util.ArrayList

class MainActivity : AppCompatActivity(), CurrenciesListDialogFragment.Listener {
    private val activeCurCodes = ArrayList<String>()
    private lateinit var currencyViewModel: CurrenciesViewModel

    private lateinit var mCurrencies: Currencies
    private lateinit var mRates: Rates
    private var mActiveCurrencyIndex = -1
    private var mSelectingCurrencyIndex = -1
    private var mActiveCurrencyAmount = ""
    private var mIsDefaultValue = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        currencyViewModel = ViewModelProvider(
            this, CurrenciesViewModel.Factory(application = application)
        ).get(CurrenciesViewModel::class.java)

        observeViewModel(currencyViewModel)

        setUpCurrencySelectorListeners()
        setUpToolTips()

        tv_currency_1_quantity.performClick()
    }

    private fun observeViewModel(viewModel: CurrenciesViewModel) {
        viewModel.currenciesLiveData.observe(this, Observer { currencies ->
            if (currencies != null) {
                mCurrencies = currencies
                viewModel.updateRates()
            }
        })

        viewModel.ratesLiveData.observe(this, Observer { rates ->
            if (rates != null) {
                mRates = rates

                setPreferredCurrencies()
                calculateConversions()
                setUpAmountListeners()
                setUpPadListeners()
            }
        })
    }

    private fun setUpToolTips() {
        TooltipCompat.setTooltipText(ll_currency_1, getString(R.string.tt_currency_1))
        TooltipCompat.setTooltipText(ll_currency_2, getString(R.string.tt_currency_2))
        TooltipCompat.setTooltipText(ll_currency_3, getString(R.string.tt_currency_3))
        TooltipCompat.setTooltipText(tv_currency_1_quantity, getString(R.string.tt_currency_active))
        TooltipCompat.setTooltipText(tv_currency_2_quantity, getString(R.string.tt_currency_active))
        TooltipCompat.setTooltipText(tv_currency_3_quantity, getString(R.string.tt_currency_active))
    }

    private fun setPreferredCurrencies() {
        // read preferences to load
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        activeCurCodes.add(sharedPref.getString("currency_code_0", "EUR") ?: return)
        activeCurCodes.add(sharedPref.getString("currency_code_1", "USD") ?: return)
        activeCurCodes.add(sharedPref.getString("currency_code_2", "JPY") ?: return)

        // load them
        loadCurrencyTo(activeCurCodes[0], 0)
        loadCurrencyTo(activeCurCodes[1], 1)
        loadCurrencyTo(activeCurCodes[2], 2)
    }

    private fun loadCurrencyTo(code: String, listPos: Int) {
        // change global variable
        activeCurCodes[listPos] = code

        // update tv
        when (listPos) {
            0 -> {
                tv_currency_1_code.text = code
                tv_currency_1_desc.text = getString(R.string.currency_desc, mCurrencies.currencies[code])
            }
            1 -> {
                tv_currency_2_code.text = code
                tv_currency_2_desc.text = getString(R.string.currency_desc, mCurrencies.currencies[code])
            }
            2 -> {
                tv_currency_3_code.text = code
                tv_currency_3_desc.text = getString(R.string.currency_desc, mCurrencies.currencies[code])
            }
        }

        // update preferences
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        getPreferences(Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putString("currency_code_$listPos", code)
            apply()
        }
    }

    private fun calculateConversions() {
        // Get conversions rates an calculate rates
        if (mActiveCurrencyIndex != -1) {
            makeConversions()
        }
    }

    private fun makeConversions() {
        val from = activeCurCodes[mActiveCurrencyIndex]
        val quantity = getAmount()
        when (mActiveCurrencyIndex) {
            0 -> {
                tv_currency_2_quantity.text = mRates.convert(quantity, from, activeCurCodes[1])
                tv_currency_3_quantity.text = mRates.convert(quantity, from, activeCurCodes[2])
            }
            1 -> {
                tv_currency_1_quantity.text = mRates.convert(quantity, from, activeCurCodes[0])
                tv_currency_3_quantity.text = mRates.convert(quantity, from, activeCurCodes[2])
            }
            2 -> {
                tv_currency_1_quantity.text = mRates.convert(quantity, from, activeCurCodes[0])
                tv_currency_2_quantity.text = mRates.convert(quantity, from, activeCurCodes[1])
            }
        }

        val formattedDate = android.text.format.DateFormat.getDateFormat(this).format(mRates.timestamp)
        tv_exchange_provider.text = getString(R.string.exchanges_provided_at, formattedDate)
    }

    private fun getAmount(): Double {
        return mActiveCurrencyAmount.replace(",", "").toDouble()
    }

    private fun setUpCurrencySelectorListeners() {
        ll_currency_1.setOnClickListener {
            mSelectingCurrencyIndex = 0
            CurrenciesListDialogFragment.newInstance(mCurrencies)
                .show(supportFragmentManager, "dialog")
        }
        ll_currency_2.setOnClickListener {
            mSelectingCurrencyIndex = 1
            CurrenciesListDialogFragment.newInstance(mCurrencies)
                .show(supportFragmentManager, "dialog")
        }
        ll_currency_3.setOnClickListener {
            mSelectingCurrencyIndex = 2
            CurrenciesListDialogFragment.newInstance(mCurrencies)
                .show(supportFragmentManager, "dialog")
        }
    }

    override fun onCurrencyClicked(code: String) {
        loadCurrencyTo(code, mSelectingCurrencyIndex)
        calculateConversions()
    }

    private fun setUpAmountListeners() {
        tv_currency_1_quantity.setOnClickListener {
            if (mActiveCurrencyIndex != 0) {
                tv_currency_1_quantity.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.colorAccent
                    )
                )
                tv_currency_2_quantity.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.color_on_background
                    )
                )
                tv_currency_3_quantity.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.color_on_background
                    )
                )

                resetActiveCurrencyValues(0, 1)
            }
        }

        tv_currency_2_quantity.setOnClickListener {
            if (mActiveCurrencyIndex != 1) {
                tv_currency_1_quantity.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.color_on_background
                    )
                )
                tv_currency_2_quantity.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.colorAccent
                    )
                )
                tv_currency_3_quantity.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.color_on_background
                    )
                )

                resetActiveCurrencyValues(1, 1)
            }
        }

        tv_currency_3_quantity.setOnClickListener {
            if (mActiveCurrencyIndex != 2) {
                tv_currency_1_quantity.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.color_on_background
                    )
                )
                tv_currency_2_quantity.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.color_on_background
                    )
                )
                tv_currency_3_quantity.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.colorAccent
                    )
                )

                resetActiveCurrencyValues(2, 1)
            }
        }

        tv_currency_1_quantity.performClick()
    }

    private fun setUpPadListeners() {
        bt_pad_0.setOnClickListener {
            addAmount("0")
        }

        bt_pad_1.setOnClickListener {
            addAmount("1")
        }

        bt_pad_2.setOnClickListener {
            addAmount("2")
        }

        bt_pad_3.setOnClickListener {
            addAmount("3")
        }
        bt_pad_4.setOnClickListener {
            addAmount("4")
        }

        bt_pad_5.setOnClickListener {
            addAmount("5")
        }
        bt_pad_6.setOnClickListener {
            addAmount("6")
        }
        bt_pad_7.setOnClickListener {
            addAmount("7")
        }
        bt_pad_8.setOnClickListener {
            addAmount("8")
        }
        bt_pad_9.setOnClickListener {
            addAmount("9")
        }
        bt_pad_comma.setOnClickListener {
            mIsDefaultValue = false
            addAmount(".")
        }
        bt_pad_0.setOnClickListener {
            addAmount("0")
        }
        bt_pad_00.setOnClickListener {
            addAmount("00")
        }
        bt_pad_backspace.setOnClickListener {
            changeActiveAmountTo(mActiveCurrencyAmount.dropLast(1))
        }
        bt_pad_ac.setOnClickListener {
            resetActiveCurrencyValues(mActiveCurrencyIndex, 0)
        }
    }

    private fun resetActiveCurrencyValues(activeIndex: Int, resetNumber: Int) {
        mActiveCurrencyIndex = activeIndex
        mActiveCurrencyAmount = "" + resetNumber
        mIsDefaultValue = true
        when (activeIndex) {
            0 -> {
                tv_currency_1_quantity.text = mActiveCurrencyAmount
            }
            1 -> {
                tv_currency_2_quantity.text = mActiveCurrencyAmount
            }
            2 -> {
                tv_currency_3_quantity.text = mActiveCurrencyAmount
            }
        }

        calculateConversions()

        if (resetNumber == 0) {
            tv_currency_1_quantity.text = "0"
            tv_currency_2_quantity.text = "0"
            tv_currency_3_quantity.text = "0"
        }
    }

    private fun addAmount(addDigit: String) {
        var amount = mActiveCurrencyAmount

        if (mIsDefaultValue) {
            amount = addDigit
            mIsDefaultValue = false
        } else {
            amount += addDigit
        }

        changeActiveAmountTo(amount)
    }

    private fun changeActiveAmountTo(amount: String) {
        if (mActiveCurrencyIndex != -1) {
            // limit length to 15
            if (amount.length > 15) {
                return
            }
            mActiveCurrencyAmount = reformatIfNeeded(amount)
            if (amount.isBlank()) {
                resetActiveCurrencyValues(mActiveCurrencyIndex, 0)
            }
            when (mActiveCurrencyIndex) {
                0 -> {
                    tv_currency_1_quantity.text = mActiveCurrencyAmount
                }
                1 -> {
                    tv_currency_2_quantity.text = mActiveCurrencyAmount
                }
                2 -> {
                    tv_currency_3_quantity.text = mActiveCurrencyAmount
                }
            }

            calculateConversions()
        }
    }

}