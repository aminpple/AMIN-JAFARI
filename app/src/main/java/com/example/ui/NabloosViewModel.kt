package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.NabloosApplication
import com.example.data.local.*
import com.example.data.repository.NabloosRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    object Idle : UiState<Nothing>
    object Loading : UiState<Nothing>
    data class Success<out T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class NabloosViewModel(private val repository: NabloosRepository) : ViewModel() {

    val cachedChandeliers: StateFlow<List<ChandelierEntity>> = repository.cachedChandeliers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cachedArticles: StateFlow<List<ArticleEntity>> = repository.cachedArticles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customDesignRequests: StateFlow<List<CustomDesignRequestEntity>> = repository.customDesignRequests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _refreshState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val refreshState: StateFlow<UiState<Unit>> = _refreshState.asStateFlow()

    private val _submissionState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val submissionState: StateFlow<UiState<String>> = _submissionState.asStateFlow()

    init {
        refreshAll()
    }

    fun refreshAll() {
        viewModelScope.launch {
            _refreshState.value = UiState.Loading
            
            // Seed base items first in case API is offline (Resilience / Iran ADSL throttle compatibility!)
            seedInitialCache()
            
            val productsOk = repository.refreshProducts()
            val articlesOk = repository.refreshArticles()
            
            if (productsOk || articlesOk) {
                _refreshState.value = UiState.Success(Unit)
            } else {
                _refreshState.value = UiState.Error("خطا در ارتباط با سرور. نمایش داده‌های ذخیره شده.")
            }
        }
    }

    private suspend fun seedInitialCache() {
        if (cachedChandeliers.value.isEmpty()) {
            repository.saveChandelier(
                ChandelierEntity(
                    id = "seed_1",
                    title = "لوستر کریستالی فلورانس ۱۲ شاخه شامپاینی",
                    category = "لوستر کلاسیک",
                    price = "۳۵,۰۰۰,۰۰۰ تومان",
                    imageUrl = "",
                    description = "لوستر کریستالی مجلل با کریستال‌های اتریشی عسلی شامپاینی درجه یک و بدنه برنز آبکاری پی‌وی‌دی طلا."
                )
            )
            repository.saveChandelier(
                ChandelierEntity(
                    id = "seed_2",
                    title = "لوستر مدرن خطی لاینر سَن‌مارکو طلایی",
                    category = "آویز دکوراتیو",
                    price = "۲۲,۵۰۰,۰۰۰ تومان",
                    imageUrl = "",
                    description = "لوستر مدرن لاینر طلایی مات ایده آل برای بالای میز ناهارخوری یا جزیره با نوارهای سیلیکونی پرنور و مدرن LED."
                )
            )
            repository.saveChandelier(
                ChandelierEntity(
                    id = "seed_3",
                    title = "آویز سقفی مدرن مدل مگنولیا مشکی",
                    category = "لوستر مدرن",
                    price = "۱۴,۸۰۰,۰۰۰ تومان",
                    imageUrl = "",
                    description = "لوستر سقفی فانتزی با بدنه فلزی کوره ای مشکی سوپرمات و شاخه های شعله ای منظم متقارن جهت فضاهای نئوکلاسیک."
                )
            )
            repository.saveChandelier(
                ChandelierEntity(
                    id = "seed_4",
                    title = "دیوارکوب کریستالی سلطنتی مارکیز تک شعله",
                    category = "چراغ دیواری مسکونی",
                    price = "۴,۲۰۰,۰۰۰ تومان",
                    imageUrl = "",
                    description = "دیوارکوب ست لوسترهای مارکیز با آبکاری باکیفیت طلایی و تک شعله حباب دار کریستال تراش دار برجسته ممتاز."
                )
            )
        }

        if (cachedArticles.value.isEmpty()) {
            repository.saveArticle(
                ArticleEntity(
                    id = "blog_1",
                    title = "مهم‌ترین نکات در انتخاب سایز مناسب لوستر پذیرایی",
                    image = "",
                    excerpt = "چگونه با توجه به ابعاد اتاق (طول، عرض) و سقف سالن، بهترین قطر و ارتفاع لوستر را محاسبه کنیم...",
                    content = "یک قانون سرانگشتی عالی برای یافتن قطر لوستر مناسب بر حسب سانتی‌متر: عرض اتاق را با طول اتاق بر حسب متر جمع کرده و حاصل را در عدد ۸٫۳ ضرب کنید تا مناسب‌ترین قطر لوستر برای شما مشخص شود...",
                    date = "۱۴۰۵/۰۳/۲۵",
                    readingTime = "۴ دقیقه"
                )
            )
            repository.saveArticle(
                ArticleEntity(
                    id = "blog_2",
                    title = "راهنمای تمیز کردن کریستال‌های لوستر برنزی بدون شستشو",
                    image = "",
                    excerpt = "روش خانگی آسان برای جلا دادن کدرترین کریستال‌های شامپاینی بدون نیاز به باز کردن قطعات لوستر...",
                    content = "یک محلول با ترکیب سه سهم آب گرم و یک سهم الکل سفید صنعتی بسازید. یک جفت دستکش پنبه ای به دست کنید، دست را در محلول خیس کرده و دانه دانه کریستال ها را پاک کنید تا برق بیفتند...",
                    date = "۱۴۰۵/۰۳/۱۸",
                    readingTime = "۶ دقیقه"
                )
            )
        }
    }

    fun submitCustomDesign(name: String, phone: String, dimensions: String, crystalType: String, comment: String) {
        viewModelScope.launch {
            if (name.isBlank() || phone.isBlank()) {
                _submissionState.value = UiState.Error("لطفاً نام و شماره همراه خود را برای هماهنگی وارد کنید.")
                return@launch
            }
            _submissionState.value = UiState.Loading
            val entity = CustomDesignRequestEntity(
                name = name,
                phone = phone,
                dimensions = dimensions,
                crystalType = crystalType,
                description = comment
            )
            val success = repository.submitDesignRequest(entity)
            if (success) {
                _submissionState.value = UiState.Success("طرح سفارشی شما با موفقیت به سرور ناب لوستر ارسال شد! بزودی کارشناسان ما به همراه الگوها با شما تماس خواهند گرفت.")
            } else {
                _submissionState.value = UiState.Success("ارتباط آنلاین برقرار نشد. طرح ارسالی شما در حافظه آفلاین گوشی ثبت شد و پس از بازگشت سیگنال ارتباطی خودکار به کارگاه فرستاده می‌شود.")
            }
        }
    }

    fun resetSubmissionState() {
        _submissionState.value = UiState.Idle
    }

    fun deleteRequest(id: Int) {
        viewModelScope.launch {
            repository.deleteDesignRequest(id)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as NabloosApplication
                return NabloosViewModel(application.repository) as T
            }
        }
    }
}
