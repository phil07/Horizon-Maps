package com.headunit.launcher

import com.mapbox.bindgen.Value

/** The pieces that tell the map where a 3D model is: its file, where it stands and which way it faces. Pure: no map state. */

internal fun modelSourceValue(uri: String, p: LL?, head: Double): Value {
    val m = HashMap<String, Value>()
    m["uri"] = Value(uri)
    m["position"] = Value(listOf(Value(p?.lon ?: 0.0), Value(p?.lat ?: 0.0)))
    m["orientation"] = Value(listOf(Value(0.0), Value(0.0), Value(head)))
    val models = HashMap<String, Value>()
    models["me"] = Value(m)
    return Value(models)
}

/** The real 3D car's files for the chosen car type (Settings > Your arrow > Car type); the saloon is the original model. */
internal fun carModelUri(part: String, model: String): String = if (model == "saloon" || model !in MapBinding.CAR_MODELS) "asset://models/car_$part.glb" else "asset://models/car_${part}_$model.glb"
