/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.tsunami.common.version;

import java.util.List;

/** Utility for version related comparison. */
final class ComparisonUtility {

  private ComparisonUtility() {}

  /**
   * Compares two lists. If one list is shorter than the other, {@code fillValue} is used for
   * comparison.
   *
   * <p>For example, comparing {@code [1, 2, 3]} and {@code [1, 2, 3, 4]} with {@code fillValue}
   * equals to 10 is equivalent of comparing {@code [1, 2, 3, 10]} with {@code [1, 2, 3, 4]}.
   *
   * @param left list for comparison.
   * @param right list for comparison.
   * @param fillValue value to use if one list is shorter than the other.
   * @param <T> element type of the list.
   * @return 0 if two lists equals, -1 if {@code left} is less than {@code right}, 1 otherwise.
   */
  static <T extends Comparable<? super T>> int compareListWithFillValue(
      List<T> left, List<T> right, T fillValue) {
    int longest = Math.max(left.size(), right.size());
    for (int i = 0; i < longest; i++) {
      T leftElement = fillValue;
      T rightElement = fillValue;

      if (i < left.size()) {
        leftElement = left.get(i);
      }
      if (i < right.size()) {
        rightElement = right.get(i);
      }

      int compareResult = leftElement.compareTo(rightElement);
      if (compareResult != 0) {
        return compareResult;
      }
    }

    return 0;
  }
}
