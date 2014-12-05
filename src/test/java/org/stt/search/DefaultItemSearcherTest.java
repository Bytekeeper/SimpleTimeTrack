package org.stt.search;

import com.google.common.base.Optional;
import org.hamcrest.collection.IsCollectionWithSize;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.experimental.theories.suppliers.TestedOn;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.ItemReaderTestHelper;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemReaderProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;

@RunWith(Theories.class)
public class DefaultItemSearcherTest {

	@Mock
	private ItemReader reader;
	private ItemReaderProvider provider;
	private ItemSearcher sut;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		provider = new ItemReaderProvider() {

			@Override
			public ItemReader provideReader() {
				return reader;
			}
		};
		sut = new DefaultItemSearcher(provider);
	}

	@Test
	public void shouldNotFindCurrentItemIfNoneCanBeRead() {
		// GIVEN
		given(reader.read()).willReturn(Optional.<TimeTrackingItem> absent());

		// WHEN
		Optional<TimeTrackingItem> result = sut.getCurrentTimeTrackingitem();

		// THEN
		assertThat(result, is(Optional.<TimeTrackingItem> absent()));
	}

	@Test
	public void shouldFindCurrentItem() {
		// GIVEN
		TimeTrackingItem unfinishedItem = new TimeTrackingItem(null,
				DateTime.now());
		given(reader.read()).willReturn(Optional.of(unfinishedItem))
				.willReturn(Optional.<TimeTrackingItem> absent());

		// WHEN
		Optional<TimeTrackingItem> result = sut.getCurrentTimeTrackingitem();

		// THEN
		assertThat(result.get(), is(unfinishedItem));
	}

	@Test
	public void shouldNotFindCurrentItemIfLastOneIsFinished() {
		// GIVEN
		TimeTrackingItem unfinishedItem = new TimeTrackingItem(null,
				DateTime.now(), DateTime.now().plusMillis(1));
		given(reader.read()).willReturn(Optional.of(unfinishedItem))
				.willReturn(Optional.<TimeTrackingItem> absent());

		// WHEN
		Optional<TimeTrackingItem> result = sut.getCurrentTimeTrackingitem();

		// THEN
		assertThat(result, is(Optional.<TimeTrackingItem> absent()));
	}

	@Test
	public void shouldOnlyFindCurrentItem() {
		// GIVEN
		TimeTrackingItem finishedItem = new TimeTrackingItem(null,
				DateTime.now(), DateTime.now().plusMillis(1));
		TimeTrackingItem unfinishedItem = new TimeTrackingItem(null,
				DateTime.now());
		given(reader.read()).willReturn(Optional.of(finishedItem))
				.willReturn(Optional.of(unfinishedItem))
				.willReturn(Optional.<TimeTrackingItem> absent());

		// WHEN
		Optional<TimeTrackingItem> result = sut.getCurrentTimeTrackingitem();

		// THEN
		assertThat(result.get(), is(unfinishedItem));
	}

	@Test
	public void allTrackedDaysShouldNotReturnSameDateTimeTwice() {
		// GIVEN
		DateTime dateTimes[] = { new DateTime(2000, 1, 1, 0, 0, 0),
				new DateTime(2000, 1, 1, 0, 0, 0) };

		givenReaderReturnsTrackingTimesForStartDates(dateTimes);

		// WHEN
		Collection<DateTime> result = sut.getAllTrackedDays();

		// THEN
		DateTime last = null;
		for (DateTime current : result) {
			assertThat(last, anyOf(nullValue(), not(is(current))));
			last = current;
		}
	}

	@Test
	public void allTrackedDaysShouldBeAtStartOfDay() {
		// GIVEN
		DateTime dateTimes[] = { new DateTime(2000, 1, 1, 3, 2, 7),
				new DateTime(2010, 1, 1, 11, 12, 13) };

		givenReaderReturnsTrackingTimesForStartDates(dateTimes);

		// WHEN
		Collection<DateTime> result = sut.getAllTrackedDays();

		// THEN
		for (DateTime time : result) {
			assertThat(time, is(time.withTimeAtStartOfDay()));
		}
	}

	@Theory
	public void allTrackedDaysShouldReturnADayPerDay(@TestedOn(ints = { 0, 1,
			3, 10 }) int days) {
		// GIVEN
		Collection<DateTime> timesForItems = new ArrayList<>();
		DateTime timeForItem = new DateTime(2000, 1, 1, 3, 2, 7);
		for (int i = 0; i < days; i++) {
			timesForItems.add(timeForItem);
			timeForItem = timeForItem.plusDays(1);
		}
		givenReaderReturnsTrackingTimesForStartDates(timesForItems
				.toArray(new DateTime[timesForItems.size()]));

		// WHEN
		Collection<DateTime> result = sut.getAllTrackedDays();

		// THEN
		assertThat(result, IsCollectionWithSize.hasSize(days));
		Iterator<DateTime> resultIt = result.iterator();
		Iterator<DateTime> timesForItemsIt = timesForItems.iterator();
		while (resultIt.hasNext() || timesForItemsIt.hasNext()) {
			DateTime trackedDay = resultIt.next();
			DateTime trackedItem = timesForItemsIt.next();
			assertThat(trackedDay, is(trackedItem.withTimeAtStartOfDay()));
		}
	}

	private void givenReaderReturnsTrackingTimesForStartDates(
			DateTime[] dateTimes) {
		TimeTrackingItem[] items = new TimeTrackingItem[dateTimes.length];
		for (int i = 0; i < dateTimes.length; i++) {
			items[i] = new TimeTrackingItem(null, dateTimes[i]);
		}
		ItemReaderTestHelper.givenReaderReturns(reader, items);
	}

}