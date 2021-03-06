package tk.kvakva.watchalarm.ui.home

import android.app.Application
import android.content.Context
import android.hardware.*
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import tk.kvakva.watchalarm.R
import java.io.IOException
import java.util.*
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt


private const val TAG = "HomeViewModel"

class HomeViewModel(private val appl: Application) : AndroidViewModel(appl), SensorEventListener {
    private var sensorManager: SensorManager =
        appl.getSystemService(Context.SENSOR_SERVICE) as SensorManager


/*    private val _text = MutableLiveData<String>().apply {
        value = "This is home Fragment"
    }
    val text: LiveData<String> = _text*/

    private var mSensorAcce: Sensor? = null

    private val _accelx0 = MutableLiveData<Float>()
    private val _accely0 = MutableLiveData<Float>()
    private val _accelz0 = MutableLiveData<Float>()
    private val _accelq0 = MutableLiveData<Float>()

    val accelx0: LiveData<Float> = _accelx0
    val accely0: LiveData<Float> = _accely0
    val accelz0: LiveData<Float> = _accelz0
    val accelq0: LiveData<Float> = _accelq0

    private val _secondprogress = MutableLiveData<Int>()
    private val _peak = MutableLiveData<Float>()

    val secondprogress: LiveData<Int> = _secondprogress
    val peak: LiveData<Float> = _peak

    //localtime in miliseconds
    private val _lastanounce = MutableLiveData(Date().time)
    val lastanounce: LiveData<Long> = _lastanounce

    init {
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            val accelSensors: List<Sensor> = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER)
            Log.v(TAG, "list of accel sensors: ${accelSensors.joinToString()}")
            mSensorAcce = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            Log.v(
                TAG,
                "mSensorAcce: ${mSensorAcce?.name} ${mSensorAcce?.vendor} ${mSensorAcce.toString()}"
            )
        } else {
            Log.v(TAG, "There is no accelerometers")
        }

        mSensorAcce?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

    }


    private val _threshold = MutableLiveData<Float>()
    val threshold: LiveData<Float> = _threshold
    var seekbarprogress = MutableLiveData<Int>()

    val sp = PreferenceManager.getDefaultSharedPreferences(appl.applicationContext)

    init {
        _threshold.value = sp.getString(appl.resources.getString(R.string.accel_threshold), "0.5111")?.toFloat()
        seekbarprogress.postValue(
            sqrt(
                (threshold.value?.times(10000) ?: 0).toFloat()
                    .div((mSensorAcce?.maximumRange ?: 0).toFloat())
            ).toInt()
        )
    }


    fun seekbarprogresstothreshold(i: Int) {
        _threshold.value = i * i * (mSensorAcce?.maximumRange?.div(10000) ?: 0).toFloat()
        with(sp.edit()) {
            putString("accel_threshold", _threshold.value.toString())
            apply()
        }
    }

    private var accel: FloatArray? = null
    private var grav: FloatArray? = null

    /**
     * Called when there is a new sensor event.  Note that "on changed"
     * is somewhat of a misnomer, as this will also be called if we have a
     * new reading from a sensor with the exact same sensor values (but a
     * newer timestamp).
     *
     *
     * See [SensorManager][android.hardware.SensorManager]
     * for details on possible sensor types.
     *
     * See also [SensorEvent][android.hardware.SensorEvent].
     *
     *
     * **NOTE:** The application doesn't own the
     * [event][android.hardware.SensorEvent]
     * object passed as a parameter and therefore cannot hold on to it.
     * The object may be part of an internal pool and may be reused by
     * the framework.
     *
     * @param event the [SensorEvent][android.hardware.SensorEvent].
     */
    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor) {
            mSensorAcce -> {
                event?.values?.let {
                    accel = it
                }

                event?.values?.let {
                    _accelx0.value = it[0]
                    _accely0.value = it[1]
                    _accelz0.value = it[2]
                    _accelq0.value = sqrt(it[0].pow(2)+it[1].pow(2)+it[2].pow(2))

                    threshold.value?.let { thrsh ->
                        _peak.value = maxOf(peak.value?:0f,accelq0.value?:0f)
                        _secondprogress.value = sqrt(
                            (peak.value?.times(10000) ?: 0).toFloat()
                                .div((mSensorAcce?.maximumRange ?: 0).toFloat())
                        ).toInt()

                        if (accelq0.value!! >= thrsh) {

                            if (Date().time.minus(lastanounce.value!!) > 1000 * 20 * 1) {
                                _lastanounce.value = Date().time
                                val client = OkHttpClient.Builder()
                                    .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                                    .writeTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                                    .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                                    .callTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                                    .addInterceptor(
                                        HttpLoggingInterceptor().apply
                                        {
                                            level = HttpLoggingInterceptor.Level.BODY
                                        }
                                    )
                                    .build()

                                val request = Request.Builder()
                                    .url(
                                        "https://api.telegram.org/bot" + sp.getString(
                                            appl.resources.getString(
                                                R.string.telegram_key
                                            ), "qwer"
                                        ) + "/sendMessage?chat_id=" + sp.getString(
                                            appl.resources.getString(
                                                R.string.telegram_chat_id
                                            ), "123123"
                                        ) + "&text=!!!!!"
                                    )
                                    .build()

                                client.newCall(request).enqueue(object : Callback {

                                    /**
                                     * Called when the request could not be executed due to cancellation, a connectivity problem or
                                     * timeout. Because networks can fail during an exchange, it is possible that the remote server
                                     * accepted the request before the failure.
                                     */
                                    override fun onFailure(call: Call, e: IOException) {
                                        Handler(Looper.getMainLooper()).post {
                                            Toast.makeText(
                                                appl.applicationContext,
                                                e.stackTraceToString(),
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }

                                    /**
                                     * Called when the HTTP response was successfully returned by the remote server. The callback may
                                     * proceed to read the response body with [Response.body]. The response is still live until its
                                     * response body is [closed][ResponseBody]. The recipient of the callback may consume the response
                                     * body on another thread.
                                     *
                                     * Note that transport-layer success (receiving a HTTP response code, headers and body) does not
                                     * necessarily indicate application-layer success: `response` may still indicate an unhappy HTTP
                                     * response code like 404 or 500.
                                     */
                                    override fun onResponse(call: Call, response: Response) {
                                        response.use {
                                            if (it.isSuccessful) {
                                                val s = JSONObject(
                                                    it.body?.string()
                                                        .orEmpty() //peekBody(2048).string().orEmpty()
                                                ).toString(4)
                                                Handler(Looper.getMainLooper()).post {
                                                    Toast.makeText(
                                                        appl.applicationContext, s,
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            } else {
                                                Handler(Looper.getMainLooper()).post {
                                                    Toast.makeText(
                                                        appl.applicationContext,
                                                        "Some Errors: $response",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }
                                        }

                                        //Toast.makeText(appl.applicationContext,JSONObject(response.body?.string().orEmpty()).toString(4),Toast.LENGTH_LONG).show()
                                    }
                                })

                            }
                        }
                    }
                }
            }
            else -> Log.v(TAG, "unknown sensor: ${event?.sensor.toString()}")
        }
    }

    /**
     * Called when the accuracy of the registered sensor has changed.  Unlike
     * onSensorChanged(), this is only called when this accuracy value changes.
     *
     *
     * See the SENSOR_STATUS_* constants in
     * [SensorManager][android.hardware.SensorManager] for details.
     *
     * @param accuracy The new accuracy of this sensor, one of
     * `SensorManager.SENSOR_STATUS_*`
     */
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.v(TAG, "onAccuracyChanges********: ${sensor.toString()} accuracy: $accuracy")
    }


}