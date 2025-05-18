package com.example.inf8405tf.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.inf8405tf.R
import com.example.inf8405tf.data.AppDatabase
import com.example.inf8405tf.databinding.FragmentHistoriqueParcoursBinding
import com.example.inf8405tf.ui.adapter.HistoriqueParcoursAdapter
import com.example.inf8405tf.ui.viewmodel.TrackingViewModel
import com.example.inf8405tf.utils.PulsatingEffectUtils
import com.example.inf8405tf.utils.TrackInfoUtils
import com.example.inf8405tf.utils.UserSession
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class HistoriqueParcoursFragment : Fragment() {

    private var _binding: FragmentHistoriqueParcoursBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var adapter: HistoriqueParcoursAdapter
    private val trackingViewModel: TrackingViewModel by activityViewModels()

    @Inject
    lateinit var database: AppDatabase

    @Inject
    lateinit var trackInfoUtils: TrackInfoUtils

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoriqueParcoursBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomSheet = view.findViewById<FrameLayout>(R.id.historique_parcours_bottom_sheet)
        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        val icExploreOffIcon = view.findViewById<ImageView>(R.id.ic_explore_off)

        if (trackingViewModel.getSelectTrackToDisplay() != null) {
            startClearMapIconSequence(icExploreOffIcon)
        }

        icExploreOffIcon.setOnClickListener {
            icExploreOffIcon.visibility = View.GONE
            trackingViewModel.triggerClearMap()
            trackingViewModel.selectTrackToDisplay(null)
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.historique_parcours_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter = HistoriqueParcoursAdapter(mutableListOf(), trackInfoUtils) { trackId ->
            // Configurer le id du parcours à afficher pour communiquer à MapsFragment
            trackingViewModel.selectTrackToDisplay(trackId)
            startClearMapIconSequence(icExploreOffIcon)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        recyclerView.adapter = adapter
        reloadTracks()

        // événement pour rafraichir la liste d'historique
        parentFragmentManager.setFragmentResultListener(
            "refresh_historique",
            viewLifecycleOwner
        ) { _, _ ->
            reloadTracks()
        }

        bottomSheetBehavior.apply {
            peekHeight =
                resources.getDimensionPixelSize(R.dimen.bottom_sheet_peek_height) // Hauteur en mode COLLAPSED
            state = BottomSheetBehavior.STATE_HALF_EXPANDED
            isDraggable = true // Permet de glisser
            isFitToContents = false // Permet de remplir l'écran en mode EXPANDED
        }

    }

    private fun reloadTracks() {
        lifecycleScope.launch {
            val historiqueParcoursBottomSheet =
                view?.findViewById<FrameLayout>(R.id.historique_parcours_bottom_sheet)
            val recyclerView =
                view?.findViewById<RecyclerView>(R.id.historique_parcours_recycler_view)
            val trackListItemLayout = view?.findViewById<FrameLayout>(R.id.track_list_item_layout)
            val emptyView = view?.findViewById<TextView>(R.id.empty_view)

            val tracks = withContext(Dispatchers.IO) {
                val username = UserSession.getUsername()
                if (username == null) {
                    Log.e("Track", "Nom d'utilisateur n'est pas défini")
                    emptyList()
                } else {
                    database.trackDao().getCompletedTracksForUser(username)
                }
            }

            adapter.updateData(tracks)

            // Vérifier si la liste est vide et mettre à jour la visibilité des vues
            if (tracks.isEmpty()) {
                recyclerView?.visibility = View.GONE
                emptyView?.visibility = View.VISIBLE

                historiqueParcoursBottomSheet?.post {
                    val currentHeight = historiqueParcoursBottomSheet.height
                    val halfHeight = currentHeight / 5

                    val layoutParams =
                        trackListItemLayout?.layoutParams as? ViewGroup.MarginLayoutParams
                    layoutParams?.topMargin = halfHeight
                    trackListItemLayout?.layoutParams = layoutParams
                }
            } else {
                recyclerView?.visibility = View.VISIBLE
                emptyView?.visibility = View.GONE

                val layoutParams =
                    trackListItemLayout?.layoutParams as? ViewGroup.MarginLayoutParams
                layoutParams?.topMargin = 0
                trackListItemLayout?.layoutParams = layoutParams
            }
        }
    }

    private fun startClearMapIconSequence(icExploreOffIcon: ImageView) {
        icExploreOffIcon.visibility = View.VISIBLE
        PulsatingEffectUtils.startPulsatingEffect(icExploreOffIcon)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}