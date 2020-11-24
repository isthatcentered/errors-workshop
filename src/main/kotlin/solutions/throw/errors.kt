package exercises.`throw`

import shared.ProspectId

data class ProspectHasNoPhoneNumberException(val id: ProspectId) : Error("Prospect ${id.value} has no phone number")

data class ProspectNotFoundException(val id: ProspectId) : Error("Prospect ${id.value} not found")

data class CouldNotSendEmailException(val error: String) : Error("Could not send email. Error: $error")
