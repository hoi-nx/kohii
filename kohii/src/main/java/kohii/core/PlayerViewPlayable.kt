/*
 * Copyright (c) 2019 Nam Nguyen, nam@ene.im
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

package kohii.core

import com.google.android.exoplayer2.ControlDispatcher
import com.google.android.exoplayer2.ui.PlayerView
import kohii.media.Media
import kohii.v1.Bridge

internal class PlayerViewPlayable(
  master: Master,
  media: Media,
  config: Config,
  bridge: Bridge<PlayerView>
) : AbstractPlayable<PlayerView>(master, media, config, bridge) {

  override fun shouldAttachRenderer(renderer: Any?) {
    require(renderer is PlayerView)
    bridge.renderer = renderer
  }

  override fun shouldDetachRenderer() {
    bridge.renderer = null
  }

  override fun considerRequestRenderer(playback: Playback) {
    super.considerRequestRenderer(playback)
    val renderer = bridge.renderer
    if (renderer != null) {
      if (playback.config.controller is ControlDispatcher) {
        renderer.setControlDispatcher(playback.config.controller)
        renderer.useController = true
      } else {
        renderer.setControlDispatcher(null)
        renderer.useController = false
      }
    }
  }

  override fun considerReleaseRenderer(playback: Playback) {
    val renderer = bridge.renderer
    if (renderer is PlayerView) {
      renderer.setControlDispatcher(null)
      renderer.useController = false
    }
    super.considerReleaseRenderer(playback)
  }
}