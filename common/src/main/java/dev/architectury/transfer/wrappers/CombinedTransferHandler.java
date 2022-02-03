/*
 * This file is part of architectury.
 * Copyright (C) 2020, 2021, 2022 architectury
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package dev.architectury.transfer.wrappers;

import com.google.common.collect.Streams;
import dev.architectury.transfer.ResourceView;
import dev.architectury.transfer.TransferAction;
import dev.architectury.transfer.TransferHandler;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface CombinedTransferHandler<T> extends TransferHandler<T> {
    Iterable<TransferHandler<T>> getHandlers();
    
    @Override
    default Stream<ResourceView<T>> getContents() {
        return Streams.stream(getHandlers()).flatMap(TransferHandler::getContents);
    }
    
    @Override
    default int getContentsSize() {
        int size = 0;
        for (TransferHandler<T> handler : getHandlers()) {
            size += handler.getContentsSize();
        }
        return size;
    }
    
    @Override
    default ResourceView<T> getContent(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("Index must be non-negative");
        }
        int i = 0;
        for (TransferHandler<T> handler : getHandlers()) {
            if (i == index) {
                return handler.getContent(0);
            }
            i += handler.getContentsSize();
        }
        throw new IndexOutOfBoundsException("Index " + index + " is out of bounds");
    }
    
    @Override
    default long insert(T toInsert, TransferAction action) {
        long toInsertAmount = getAmount(toInsert);
        long amount = toInsertAmount;
        for (TransferHandler<T> handler : getHandlers()) {
            long inserted = handler.insert(copyWithAmount(toInsert, amount), action);
            amount -= inserted;
            if (amount <= 0) {
                break;
            }
        }
        return toInsertAmount - amount;
    }
    
    @Override
    default T extract(T toExtract, TransferAction action) {
        // TODO: Implement
        throw new UnsupportedOperationException();
    }
    
    @Override
    default T extract(Predicate<T> toExtract, long maxAmount, TransferAction action) {
        // TODO: Implement
        throw new UnsupportedOperationException();
    }
    
    long getAmount(T resource);
    
    @Override
    default Object saveState() {
        Iterable<TransferHandler<T>> handlers = getHandlers();
        if (handlers instanceof Collection) {
            Object[] states = new Object[((Collection<TransferHandler<T>>) handlers).size()];
            int i = 0;
            for (TransferHandler<T> handler : handlers) {
                states[i++] = handler.saveState();
            }
            return states;
        }
        return Streams.stream(handlers).map(TransferHandler::saveState).toArray();
    }
    
    @Override
    default void loadState(Object state) {
        if (state instanceof Object[]) {
            int i = 0;
            for (TransferHandler<T> handler : getHandlers()) {
                handler.loadState(((Object[]) state)[i++]);
            }
        } else {
            throw new IllegalArgumentException("Invalid state type: " + state.getClass());
        }
    }
}
