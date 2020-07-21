class LocationInfo(val id: Int, val name: String) {

    companion object {
        fun withFields(id: Int?, name: String?): LocationInfo? {
            return if (id == null || name == null) {
                null
            } else LocationInfo(id, name)
        }
    }

}