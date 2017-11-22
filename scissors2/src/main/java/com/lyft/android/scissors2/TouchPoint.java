/*
 * Copyright (C) 2015 Lyft, Inc.
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
package com.lyft.android.scissors2;

class TouchPoint {

    private float x;
    private float y;

    public TouchPoint() {
    }

    public TouchPoint(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getLength() {
        return (float) Math.sqrt(x * x + y * y);
    }

    public TouchPoint copy(TouchPoint other) {
        x = other.getX();
        y = other.getY();
        return this;
    }

    public TouchPoint set(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public TouchPoint add(TouchPoint value) {
        this.x += value.getX();
        this.y += value.getY();
        return this;
    }

    public static TouchPoint subtract(TouchPoint lhs, TouchPoint rhs) {
        return new TouchPoint(lhs.x - rhs.x, lhs.y - rhs.y);
    }

    @Override
    public String toString() {
        return String.format("(%.4f, %.4f)", x, y);
    }
}
