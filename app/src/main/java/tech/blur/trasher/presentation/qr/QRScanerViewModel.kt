package tech.blur.trasher.presentation.qr

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject
import retrofit2.HttpException
import tech.blur.trasher.common.Result
import tech.blur.trasher.common.rx.SchedulerProvider
import tech.blur.trasher.common.toResult
import tech.blur.trasher.data.api.TrasherApi
import tech.blur.trasher.domain.Parcel
import tech.blur.trasher.presentation.BaseViewModel

class QRScanerViewModel(
    api: TrasherApi,
    schedulerProvider: SchedulerProvider
): BaseViewModel() {

    val registerParcelsSubject = PublishSubject.create<Parcel>()

    private val mutableNetworkProgress = MutableLiveData<Boolean>()
    val networkProgress: LiveData<Boolean> = mutableNetworkProgress

    private val mutableErrorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = mutableErrorMessage

    private val mutableRegistrationResult = MutableLiveData<Int>()
    val registrationResult: LiveData<Int> = mutableRegistrationResult

    init {
        registerParcelsSubject
            .doOnNext { mutableNetworkProgress.value = true }
            .flatMapSingle { api.registeParcels(it).toResult() }
            .observeOn(schedulerProvider.ui())
            .subscribe {
                when (it) {
                    is Result.Success -> {
                        mutableRegistrationResult.value = it.data.data
                    }
                    is Result.Failure -> {
                        mutableRegistrationResult.value = -1

                        when (it.throwable) {
                            is HttpException -> {
                                if (it.throwable.code() == 503)
                                    mutableErrorMessage.value = "Этот QR код уже был испльзован"
                                else
                                    mutableErrorMessage.value = "Какие то проблемы с сетью("
                            }
                            else -> {
                                mutableErrorMessage.value = it.throwable.message
                            }
                        }
                    }
                }
                mutableNetworkProgress.value = false
            }.addTo(compositeDisposable)
    }

    //TODO Добавление пакета
}