/*
 * Copyright 2015 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.logic.nameTags;

import org.terasology.entitySystem.Component;

/**
 * Give this component to a player controlled character to make it show the player name in his player color above
 * his head.
 *
 * This compoennt will implicitly add a {@link NameTagComponent} and will automatically update it.
 */
public class PlayerNameTagComponent implements Component {

    /**
     * Height offset from the origin of the character.
     */
    public float yOffset = 1.0f;

}
