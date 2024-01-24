package jp.co.cyberagent.android.gpuimage

fun main() {
//top right  193, 55, 159, 55, 65, 159, 65, 52, 159, 52, 53, 159, 53, 46, 159, 46, 124, 159, 124, 35, 159, 159,145,193, 193,145,245
//    val points = arrayListOf(193,55,65,52,53,46,124,35)
//    val target = 159
// bottom right  111, 117, 145, 117, 118, 145, 118, 119, 145, 119, 120, 145, 120, 121, 145, 121, 128, 145, 128, 245, 145, 145,159,35, 35,111,145
//    val points = arrayListOf(111, 117, 118, 119, 120, 121, 128, 245)
//    val target = 145
    val points = arrayListOf(417, 285, 295, 282, 283, 276, 353, 265)
    val target = 386
    createTriangle(points, target)
    val points1 = arrayListOf(465,357,350,349,348,347,346,340,)
    val target1 = 374
    createTriangle(points1, target1)
}

fun createTriangle(point: ArrayList<Int>, target: Int) {
    val triangle = arrayListOf<Int>()
    repeat(point.size - 1) {
        if (it <= point.size - 2) {
            triangle.add(point[it])
            triangle.add(point[it + 1])
            triangle.add(target)
        }
    }

    println("triangle is    ${triangle.joinToString()}")
}