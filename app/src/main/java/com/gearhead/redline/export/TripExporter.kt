package com.gearhead.redline.export

import com.gearhead.redline.data.local.entity.TripWithPoints

/**
 * Export surface, intentionally left as a contract for a future milestone
 * (out of scope for the MVP). Implementations will serialize a [TripWithPoints]
 * to GPX or CSV. Wire a concrete implementation into the detail screen's
 * overflow menu when the feature is scheduled.
 */
interface TripExporter {
    /** Serialize the trip to bytes in the exporter's format. */
    fun export(trip: TripWithPoints): ByteArray

    val fileExtension: String
    val mimeType: String
}

/** GPX 1.1 track export. TODO: implement (out of MVP scope). */
class GpxTripExporter : TripExporter {
    override val fileExtension: String = "gpx"
    override val mimeType: String = "application/gpx+xml"
    override fun export(trip: TripWithPoints): ByteArray =
        TODO("GPX export is planned for a later milestone")
}

/** Flat CSV of route points. TODO: implement (out of MVP scope). */
class CsvTripExporter : TripExporter {
    override val fileExtension: String = "csv"
    override val mimeType: String = "text/csv"
    override fun export(trip: TripWithPoints): ByteArray =
        TODO("CSV export is planned for a later milestone")
}
