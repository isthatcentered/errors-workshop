package shared

data class InviteLink(val link: String)

data class ProspectId(val value: String) {
	// Smart constructor pattern:
	// Never allow construction of incorrect data
	init {
		if (!isValid())
			throw  Error("Invalid ProspectId: '$value'")
	}

	private fun isValid(): Boolean {
		return true
	}
}

data class InviteProspectParams(val prospectId: String)

data class MailTemplate(val subject: String, val body: String)

data class SMSTemplate(val body: String)
