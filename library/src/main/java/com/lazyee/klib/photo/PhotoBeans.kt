package com.lazyee.klib.photo.bean

/**
 * @Author leeorz
 * @Date 3/11/21-3:54 PM
 * @Description:照片实体
 */
data class Photo(val path: String? = null,
                 val width: Int? = null,
                 val height: Int? = null,
                 val id: Int? = null) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Photo) return false
        return other.path == path
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}

/**
 * 照片文件夹实体
 * @property id String?
 * @property coverPath String?
 * @property name String?
 * @property addedDate Long
 * @property photos MutableList<Photo>
 * @constructor
 */
data class PhotoDirectory(
    var id: String? = null,
    var coverPath: String? = null,
    var name: String? = null,
    var addedDate: Long = 0,
    var photos: MutableList<Photo> = mutableListOf(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PhotoDirectory) return false
        if (id == null) return false
        if (id != other.id) return false
        if (name == null) return false
        return name == other.name
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        if (name == null) {
            return result
        }
        result = 31 * result + name.hashCode()
        return result
    }

    fun addPhoto(id: Int, path: String?) {
        photos.add(Photo(id = id, path = path))
    }
}