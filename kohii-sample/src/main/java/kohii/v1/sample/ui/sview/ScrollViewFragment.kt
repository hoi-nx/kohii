/*
 * Copyright (c) 2018 Nam Nguyen, nam@ene.im
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kohii.v1.sample.ui.sview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.SharedElementCallback
import androidx.core.view.ViewCompat
import androidx.transition.TransitionInflater
import androidx.transition.TransitionSet
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.material.snackbar.Snackbar
import kohii.v1.Kohii
import kohii.v1.Playback
import kohii.v1.PlaybackEventListener
import kohii.v1.PlayerEventListener
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.ui.player.PlayerDialogFragment
import kohii.v1.sample.ui.player.PlayerFragment
import kotlinx.android.synthetic.main.fragment_scroll_view.playerContainer
import kotlinx.android.synthetic.main.fragment_scroll_view.playerView

class ScrollViewFragment : BaseFragment(),
    Playback.Callback,
    PlaybackEventListener,
    PlayerDialogFragment.Callback {

  companion object {
    const val pageTagKey = "kohii:demo:page:tag"
    const val videoUrl =
      "https://bitdash-a.akamaihd.net/content/MI201109210084_1/m3u8s/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.m3u8"

    fun newInstance() = ScrollViewFragment().also {
      it.arguments = Bundle()
    }

    fun newInstance(tag: String) = newInstance().also {
      it.arguments?.putString(pageTagKey, tag)
    }
  }

  private var playback: Playback<PlayerView>? = null
  private val listener = object : PlayerEventListener {
    override fun onVideoSizeChanged(
      width: Int,
      height: Int,
      unappliedRotationDegrees: Int,
      pixelWidthHeightRatio: Float
    ) {
      startPostponedEnterTransition()
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    return inflater.inflate(R.layout.fragment_scroll_view, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    prepareTransitions()
    postponeEnterTransition()

    playback = Kohii[this].setUp(videoUrl)
        .copy(repeatMode = Player.REPEAT_MODE_ONE)
        .copy(tag = this.arguments?.get(pageTagKey) ?: videoUrl)
        .asPlayable()
        .bind(playerView)
        .also { it.observe(viewLifecycleOwner) }

    val transView: View = playerView.findViewById(R.id.exo_content_frame)
    ViewCompat.setTransitionName(transView, videoUrl)
  }

  override fun onStart() {
    super.onStart()
    playback?.also {
      it.addPlaybackEventListener(this@ScrollViewFragment)
      it.addCallback(this@ScrollViewFragment)
      it.addPlayerEventListener(listener)
    }

    view?.run {
      val transView: View = playerView.findViewById(R.id.exo_content_frame)
      playerContainer.setOnClickListener {
        (exitTransition as TransitionSet).excludeTarget(this, true)
        fragmentManager!!.beginTransaction()
            .setReorderingAllowed(true)
            .addSharedElement(transView, ViewCompat.getTransitionName(transView)!!)
            .replace(R.id.fragmentContainer, PlayerFragment.newInstance(videoUrl), videoUrl)
            .addToBackStack(null)
            .commit()
      }

      /* playerContainer.setOnClickListener {
        PlayerDialogFragment.newInstance(videoUrl)
            .show(childFragmentManager, videoUrl)
      } */
    }
  }

  @Suppress("RedundantOverride")
  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    // ⬇︎ For demo of manual fullscreen.
    // requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
  }

  override fun onStop() {
    super.onStop()
    playback?.removeCallback(this)
    playback?.removePlaybackEventListener(this)
    playback?.removePlayerEventListener(listener)
    playerContainer.setOnClickListener(null)
  }

  private fun prepareTransitions() {
    // Hmm Google https://stackoverflow.com/questions/49461738/transitionset-arraylist-size-on-a-null-object-reference
    val transition = TransitionInflater.from(requireContext())
        .inflateTransition(R.transition.player_exit_transition)
    transition.duration = 375
    exitTransition = transition

    // A similar mapping is set at the ImagePagerFragment with a setEnterSharedElementCallback.
    setEnterSharedElementCallback(object : SharedElementCallback() {
      override fun onMapSharedElements(
        names: MutableList<String>?,
        sharedElements: MutableMap<String, View>?
      ) {
        // Map the first shared element name to the child ImageView.
        sharedElements?.put(names?.get(0)!!, playerView.findViewById(R.id.exo_content_frame))
      }
    })
  }

  // BEGIN: PlaybackEventListener

  override fun onFirstFrameRendered() {
  }

  override fun onBuffering(playWhenReady: Boolean) {
  }

  override fun onPlaying() {
    view?.run {
      Snackbar.make(this, "State: Playing", Snackbar.LENGTH_LONG)
          .show()
    }
  }

  override fun onPaused() {
    view?.run {
      Snackbar.make(this, "State: Paused", Snackbar.LENGTH_LONG)
          .show()
    }
  }

  override fun onCompleted() {
    view?.run {
      Snackbar.make(this, "State: Ended", Snackbar.LENGTH_LONG)
          .show()
    }
  }

  // END: PlaybackEventListener

  // BEGIN: Playback.Callback

  override fun onActive(
    playback: Playback<*>,
    target: Any?
  ) {
    Toast.makeText(requireContext(), "Target available", Toast.LENGTH_SHORT)
        .show()
    startPostponedEnterTransition()
  }

  override fun onInActive(
    playback: Playback<*>,
    target: Any?
  ) {
    Toast.makeText(requireContext(), "Target unavailable", Toast.LENGTH_SHORT)
        .show()
  }

  // END: Playback.Callback

  override fun onDialogActive(tag: Any) {
  }

  override fun onDialogInActive(tag: Any) {
    Kohii[this].findPlayable(tag)
        ?.bind(playerView)
  }
}
