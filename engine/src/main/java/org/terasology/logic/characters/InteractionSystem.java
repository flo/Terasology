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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.asset.AssetType;
import org.terasology.asset.AssetUri;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.EventPriority;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.input.ButtonState;
import org.terasology.input.binds.inventory.InventoryButton;
import org.terasology.logic.characters.events.InteractionEndEvent;
import org.terasology.logic.characters.events.InteractionEndRequest;
import org.terasology.logic.characters.events.InteractionStartEvent;
import org.terasology.logic.characters.events.InteractionStartRequest;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.network.ClientComponent;
import org.terasology.registry.In;
import org.terasology.rendering.nui.NUIManager;
import org.terasology.rendering.nui.ScreenLayerClosedEvent;

/**
 *
 * @author Immortius <immortius@gmail.com>
 * @author Florian <florian@fkoeberle.de>
 *
 */
@RegisterSystem(RegisterMode.ALWAYS)
public class InteractionSystem extends BaseComponentSystem {
    private static final Logger logger = LoggerFactory.getLogger(InteractionSystem.class);

    @In
    private NUIManager nuiManager;

    @ReceiveEvent(components = {InteractionScreenComponent.class}, netFilter = RegisterMode.AUTHORITY)
    public void onActivate(ActivateEvent event, EntityRef entity) {
        EntityRef instigator = event.getInstigator();

        CharacterComponent characterComponent = instigator.getComponent(CharacterComponent.class);
        if (characterComponent == null) {
            return;
        }

        if (characterComponent.interactionTarget.exists()) {
            InteractionUtil.setInteractionTarget(instigator, entity);
        }


        InteractionUtil.setInteractionTarget(instigator, entity);

    }

    @ReceiveEvent(components = {InteractionScreenComponent.class})
    public void onInteractionStart(InteractionStartEvent event, EntityRef container,
                                   InteractionScreenComponent interactionScreenComponent) {
        EntityRef investigator = event.getInstigator();
        CharacterComponent characterComponent = investigator.getComponent(CharacterComponent.class);
        if (characterComponent == null) {
            logger.error("Interaction started by entity without character component");
            return;
        }
        ClientComponent controller = characterComponent.controller.getComponent(ClientComponent.class);
        if (controller != null && controller.local) {
            nuiManager.pushScreen(interactionScreenComponent.screen);
        }
    }

    @ReceiveEvent(components = {InteractionScreenComponent.class})
    public void onInteractionEnd(InteractionEndEvent event, EntityRef container,
                                 InteractionScreenComponent interactionScreenComponent) {
        EntityRef investigator = event.getInstigator();
        CharacterComponent characterComponent = investigator.getComponent(CharacterComponent.class);
        if (characterComponent == null) {
            logger.error("Interaction started by entity without character component");
            return;
        }
        ClientComponent controller = characterComponent.controller.getComponent(ClientComponent.class);
        if (controller != null && controller.local) {
            nuiManager.closeScreen(interactionScreenComponent.screen);
        }
    }


    /**
     * The method listens for the event that the user closes the screen of the current interaction target.
     *
     * When it happens it updates the interactionTarget field via
     * {@link InteractionUtil#setInteractionTarget(EntityRef,EntityRef)}.
     */
    @ReceiveEvent(components = {ClientComponent.class})
    public void onScreenLayerClosed(ScreenLayerClosedEvent event, EntityRef container, ClientComponent clientComponent) {
        EntityRef character = clientComponent.character;
        AssetUri activeInteractionScreenUri = InteractionUtil.getActiveInteractionScreenUri(character);

        if ((activeInteractionScreenUri != null) && (activeInteractionScreenUri.equals(event.getClosedScreenUri()))) {
            InteractionUtil.setInteractionTarget(clientComponent.character, EntityRef.NULL);
        }
    }


    /*
     * At the activation of the inventory the current dialog needs to be closed instantly.
     *
     * The close of the dialog triggers {@link #onScreenLayerClosed} which resets the
     * interactionTarget.
     */
    @ReceiveEvent(components = ClientComponent.class, priority = EventPriority.PRIORITY_HIGH)
    public void onToggleInventory(InventoryButton event, EntityRef entity, ClientComponent clientComponent) {
        if (event.getState() != ButtonState.DOWN) {
            return;
        }

        EntityRef character = clientComponent.character;
        AssetUri activeInteractionScreenUri = InteractionUtil.getActiveInteractionScreenUri(character);
        if (activeInteractionScreenUri != null) {
            nuiManager.closeScreen(activeInteractionScreenUri);
            // do not consume the event, so that the inventory will still open
        }
    }

    @ReceiveEvent(components = {}, netFilter = RegisterMode.AUTHORITY)
    public void onInteractionStartRequest(InteractionStartRequest request, EntityRef instigator) {
        EntityRef target = request.getTarget();
        target.send(new InteractionStartEvent(instigator));
    }


    /**
     *
     * Sets interactionTarget to the specified value with the highest priority so that the field is updated when
     * other handlers of this event run.
     */
    @ReceiveEvent(components = {}, netFilter = RegisterMode.ALWAYS, priority = EventPriority.PRIORITY_CRITICAL)
    public void onInteractionStartEvent(InteractionStartEvent event, EntityRef target) {
        EntityRef instigator = event.getInstigator();
        CharacterComponent characterComponent = instigator.getComponent(CharacterComponent.class);
        if (characterComponent == null) {
            logger.error("Interaction start request instigator has no character component");
            return;
        }
        if (characterComponent.interactionTarget.exists()) {
            logger.error("Interaction wasn't finished at start of next interaction");
            target.send(new InteractionEndEvent(instigator));
        }

        characterComponent.interactionTarget = target;
        instigator.saveComponent(characterComponent);
    }

    @ReceiveEvent(components = {}, netFilter = RegisterMode.AUTHORITY)
    public void onInteractionEndRequest(InteractionEndRequest request, EntityRef instigator) {
        EntityRef target = request.getTarget();
        target.send(new InteractionEndEvent(instigator));
    }

    /**
     * Sets interactionTarget to NULL with a low priorty so that all handlers of this event are finished when this event
     * gets processed.
     */
    @ReceiveEvent(components = {}, netFilter = RegisterMode.ALWAYS, priority = EventPriority.PRIORITY_TRIVIAL)
    public void onInteractionEndEent(InteractionEndEvent event, EntityRef target) {
        EntityRef instigator = event.getInstigator();
        CharacterComponent characterComponent = instigator.getComponent(CharacterComponent.class);
        if (characterComponent == null) {
            logger.error("Interaction end request instigator has no character component");
            return;
        }

        characterComponent.interactionTarget = EntityRef.NULL;
        instigator.saveComponent(characterComponent);
    }

}