package epicarchitect.thelastarch

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import epicarchitect.thelastarch.ui.PokemonsFragment

class MainActivity : AppCompatActivity(R.layout.main_activity) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, PokemonsFragment::class.java, null)
                .commit()
        }
    }
}