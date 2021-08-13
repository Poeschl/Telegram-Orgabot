package services

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.ValueRange
import mu.KotlinLogging
import java.io.File
import java.nio.file.Path
import kotlin.io.path.pathString


class GoogleSheetService(private val configFolder: Path, private val configService: ConfigService) {

    companion object {
        private val LOGGER = KotlinLogging.logger { }
        private val SCOPES = listOf(SheetsScopes.SPREADSHEETS_READONLY)
    }

    private val jsonFactory = GsonFactory()


    fun getLocations(): List<Location> {
        val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
        val sheetData = getLocationValues(
            httpTransport, configService.config.locationPoll.sheetId,
            configService.config.locationPoll.namesArea, configService.config.locationPoll.tagsArea
        )
        val names = sheetData?.get(0)?.getValues()?.map { if (it.isEmpty()) null else it[0] }
        val tags = sheetData?.get(1)?.getValues()?.map { if (it.isEmpty()) null else it[0] }

        val locations = mutableListOf<Location>()
        if (names != null && tags != null) {
            names.forEachIndexed { index, name ->
                val locationName = name as String
                val locationTagString = tags.getOrNull(index)
                val locationTags = if (locationTagString != null) {
                    (locationTagString as String).split(",").map { it.lowercase().trim() }
                } else {
                    emptyList()
                }

                LOGGER.debug { "Got $name (${locationTags.joinToString(", ")})" }

                locations.add(Location(locationName, locationTags))
            }
        }
        return locations
    }

    fun getTags(): List<String> {
        val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
        val sheetData = getTags(httpTransport, configService.config.locationPoll.sheetId, configService.config.locationPoll.tagsArea)
        val tags = sheetData?.mapNotNull { if (it.isEmpty()) null else it[0] }
            ?.flatMap { it.toString().split(",").map { tag -> tag.trim().lowercase() } }
            ?.distinct()
            .orEmpty()
        LOGGER.debug { "Got tags ${tags.joinToString(", ")}" }
        return tags
    }

    private fun getLocationValues(httpTransport: NetHttpTransport, sheetId: String, nameRange: String, tagRange: String): MutableList<ValueRange>? {
        val sheetsService = Sheets.Builder(httpTransport, jsonFactory, getCredentials(httpTransport)).setApplicationName("Orgabot").build()
        val response = sheetsService.spreadsheets().values().batchGet(sheetId).setRanges(listOf(nameRange, tagRange)).execute()
        return response.valueRanges
    }

    private fun getTags(httpTransport: NetHttpTransport, sheetId: String, tagRange: String): MutableList<MutableList<Any>>? {
        val sheetsService = Sheets.Builder(httpTransport, jsonFactory, getCredentials(httpTransport)).setApplicationName("Orgabot").build()
        val response = sheetsService.spreadsheets().values().get(sheetId, tagRange).execute()
        return response.getValues()
    }

    private fun getCredentials(httpTransport: NetHttpTransport): Credential? {
        val credentialFile = File("${configFolder.pathString}/${configService.config.locationPoll.googleCredentialPath}")
        if (!credentialFile.exists()) {
            LOGGER.error { "Could not find the credentials file at '${credentialFile.path}'" }
            return null
        }
        return GoogleCredential.fromStream(credentialFile.inputStream()).createScoped(SCOPES)
    }

    data class Location(val name: String, val tags: List<String>)
}
