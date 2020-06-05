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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link ComparisonUtility}. */
@RunWith(JUnit4.class)
public final class ComparisonUtilityTest {

  @Test
  public void compareWithFillValue_bothEmptyListWithFillValueEqualToZero_returnsZero() {
    assertThat(
            ComparisonUtility.compareListWithFillValue(
                Lists.newArrayList(), Lists.newArrayList(), 0))
        .isEqualTo(0);
  }

  @Test
  public void compareWithFillValue_bothEmptyListWithPositiveFillValue_returnsZero() {
    assertThat(
            ComparisonUtility.compareListWithFillValue(
                Lists.newArrayList(), Lists.newArrayList(), 1))
        .isEqualTo(0);
  }

  @Test
  public void compareWithFillValue_bothEmptyListWithNegativeFilValue_returnsZero() {
    assertThat(
            ComparisonUtility.compareListWithFillValue(
                Lists.newArrayList(), Lists.newArrayList(), -1))
        .isEqualTo(0);
  }

  @Test
  public void compareWithFillValue_oneEmptyListAndSmallFillValue_returnsNegative() {
    assertThat(
            ComparisonUtility.compareListWithFillValue(
                Lists.newArrayList(), Lists.newArrayList(1, 2, 3), 0))
        .isLessThan(0);
  }

  @Test
  public void compareWithFillValue_oneEmptyListAndLargeFillValue_returnsPositive() {
    assertThat(
            ComparisonUtility.compareListWithFillValue(
                Lists.newArrayList(), Lists.newArrayList(1, 2, 3), 100))
        .isGreaterThan(0);
  }

  @Test
  public void compareWithFillValue_nonEmptyListSameSizeGreaterValue_returnsPositive() {
    assertThat(
            ComparisonUtility.compareListWithFillValue(
                Lists.newArrayList(1, 3, 4), Lists.newArrayList(1, 2, 3), 100))
        .isGreaterThan(0);
  }

  @Test
  public void compareWithFillValue_nonEmptyListSameSizeEqualValue_returnsZero() {
    assertThat(
            ComparisonUtility.compareListWithFillValue(
                Lists.newArrayList(1, 2, 3), Lists.newArrayList(1, 2, 3), 100))
        .isEqualTo(0);
  }

  @Test
  public void compareWithFillValue_nonEmptyListSameSizeLessThanValue_returnsNegative() {
    assertThat(
            ComparisonUtility.compareListWithFillValue(
                Lists.newArrayList(1, 1, 3), Lists.newArrayList(1, 2, 3), 100))
        .isLessThan(0);
  }

  @Test
  public void compareWithFillValue_nonEmptyListVariedSizeWithPositiveFillValue_returnsNegative() {
    assertThat(
            ComparisonUtility.compareListWithFillValue(
                Lists.newArrayList(1, 1), Lists.newArrayList(1, 2, 3), 100))
        .isLessThan(0);
  }

  @Test
  public void compareWithFillValue_nonEmptyListVariedSizeWithZeroFillValue_returnsPositive() {
    assertThat(
            ComparisonUtility.compareListWithFillValue(
                Lists.newArrayList(1, 3), Lists.newArrayList(1, 2, 3), 0))
        .isGreaterThan(0);
  }

  @Test
  public void compareWithFillValue_nonEmptyListVariedSizeWithZeroFillValue_returnsNegative() {
    assertThat(
            ComparisonUtility.compareListWithFillValue(
                Lists.newArrayList(1, 2), Lists.newArrayList(1, 2, 3), 0))
        .isLessThan(0);
  }

  @Test
  public void compareWithFillValue_nonEmptyListVariedSizeWithPositiveFillValue_returnsPositive() {
    assertThat(
            ComparisonUtility.compareListWithFillValue(
                Lists.newArrayList(1, 2), Lists.newArrayList(1, 2, 3), 100))
        .isGreaterThan(0);
  }
}
