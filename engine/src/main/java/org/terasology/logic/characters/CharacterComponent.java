/*
 * Copyright 2013 MovingBlocks
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
package org.terasology.logic.characters;

import com.bulletphysics.linearmath.QuaternionUtil;
import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.Owns;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.characters.interactions.InteractionStartEvent;
import org.terasology.math.Direction;
import org.terasology.math.TeraMath;
import org.terasology.network.FieldReplicateType;
import org.terasology.network.Replicate;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

/**
 * Information common to characters (the physical body of players and creatures)
 *
 * @author Immortius <immortius@gmail.com>
 */
public final class CharacterComponent implements Component {
    public Vector3f spawnPosition = new Vector3f();
    public float eyeOffset = 0.6f;
    /**
     * Specifies the maximium range at which this character is able to interact with other objects.
     * When the player leaves the range the interaction gets canceled.
     */
    public float interactionRange = 5f;
    /**
     * Specifies the current interaction target. It points for example to a standard chest when the player has opened it.
     *
     * Important: Use {@link CharacterUtil#setInteractionTarget(EntityRef, EntityRef)}} to set this value, so that the
     * value.
     *
     * This {@link InteractionStartEvent} is sent to all clients when a
     */
    @Replicate(FieldReplicateType.SERVER_TO_CLIENT)
    public EntityRef interactionTarget = EntityRef.NULL;
    public float pitch;
    public float yaw;

    @Replicate(FieldReplicateType.SERVER_TO_OWNER)
    @Owns
    public EntityRef movingItem = EntityRef.NULL;

    @Replicate(FieldReplicateType.SERVER_TO_OWNER)
    public EntityRef controller = EntityRef.NULL;

    // What inventory slot the character has selected (this currently also determines held item, will need to review based on gameplay)
    public int selectedItem;
    public float handAnimation;

    public Quat4f getLookRotation() {
        Quat4f lookRotation = new Quat4f();
        QuaternionUtil.setEuler(lookRotation, TeraMath.DEG_TO_RAD * yaw, TeraMath.DEG_TO_RAD * pitch, 0);
        return lookRotation;
    }

    public Vector3f getLookDirection() {
        Vector3f result = Direction.FORWARD.getVector3f();
        QuaternionUtil.quatRotate(getLookRotation(), result, result);
        return result;
    }
}
