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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.lifecycleEvents.BeforeDeactivateComponent;
import org.terasology.entitySystem.entity.lifecycleEvents.OnActivatedComponent;
import org.terasology.entitySystem.entity.lifecycleEvents.OnChangedComponent;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.common.DisplayNameComponent;
import org.terasology.network.ClientComponent;
import org.terasology.network.ClientInfoComponent;
import org.terasology.network.ColorComponent;
import org.terasology.network.NetworkSystem;
import org.terasology.registry.In;
import org.terasology.rendering.nui.Color;

@RegisterSystem(RegisterMode.AUTHORITY)
public class PlayerNameTagSystem extends BaseComponentSystem {
    private static final Logger logger = LoggerFactory.getLogger(NameTagClientSystem.class);

    @In
    private NetworkSystem networkSystem;

    private BiMap<EntityRef, EntityRef> clientInfoToPlayerTagOwningEntityMap = HashBiMap.create();

    @ReceiveEvent
    public void onPlayerNameTagActivation(OnActivatedComponent event, EntityRef characterEntity,
                                            PlayerNameTagComponent playerNameTagComponent) {

        EntityRef ownerEntity = networkSystem.getOwnerEntity(characterEntity);
        if (ownerEntity == null) {
            return; // NPC
        }

        ClientComponent clientComponent = ownerEntity.getComponent(ClientComponent.class);
        if (clientComponent == null) {
            logger.warn("Can't create player based name tag for character as owner has no client component");
            return;
        }
        EntityRef clientInfoEntity = clientComponent.clientInfo;

        DisplayNameComponent displayNameComponent = clientInfoEntity.getComponent(DisplayNameComponent.class);
        if (displayNameComponent == null) {
            logger.error("Can't create player based name tag for character as client info has no DisplayNameComponent");
            return;
        }
        String name = displayNameComponent.name;

        Color color = Color.WHITE;
        ColorComponent colorComponent = clientInfoEntity.getComponent(ColorComponent.class);
        if (colorComponent != null) {
            color = colorComponent.color;
        }

        float yOffset = playerNameTagComponent.yOffset;

        NameTagComponent nameTagComponent = characterEntity.getComponent(NameTagComponent.class);
        boolean newComponent = nameTagComponent == null;
        if (nameTagComponent == null) {
            nameTagComponent = new NameTagComponent();
        }
        nameTagComponent.text = name;
        nameTagComponent.textColor = color;
        nameTagComponent.yOffset = yOffset;
        if (newComponent) {
            characterEntity.addComponent(nameTagComponent);
        } else {
            characterEntity.saveComponent(nameTagComponent);
        }

        clientInfoToPlayerTagOwningEntityMap.put(clientInfoEntity, characterEntity);
    }


    @ReceiveEvent(components = {ClientInfoComponent.class })
    public void onDisplayNameChange(OnChangedComponent event, EntityRef entity,
                                    DisplayNameComponent displayNameComponent,
                                    ClientInfoComponent clientInfoComponent) {

        EntityRef nameTagOwningCharacterEntity = clientInfoToPlayerTagOwningEntityMap.get(entity);
        if (nameTagOwningCharacterEntity != null) {
            NameTagComponent nameTagComponent = nameTagOwningCharacterEntity.getComponent(NameTagComponent.class);
            if (nameTagComponent != null) {
                nameTagComponent.text = displayNameComponent.name;
                nameTagOwningCharacterEntity.saveComponent(nameTagComponent);
            } else {
                logger.warn("Tried to update the name tag component with a new player name but it was missing");
            }
        }

    }


    @ReceiveEvent(components = {PlayerNameTagComponent.class })
    public void onPlayerNameTagDeactivated(BeforeDeactivateComponent event, EntityRef entity) {
        entity.removeComponent(NameTagComponent.class);
        clientInfoToPlayerTagOwningEntityMap.inverse().remove(entity);
    }

}
