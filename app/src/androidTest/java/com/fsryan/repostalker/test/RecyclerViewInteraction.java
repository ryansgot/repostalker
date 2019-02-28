package com.fsryan.repostalker.test;

import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.ViewInteraction;
import android.view.View;

import org.hamcrest.Matcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static android.support.test.espresso.contrib.RecyclerViewActions.scrollToPosition;

/**
 * Originally copied from here: https://gist.github.com/RomainPiel/ec10302a4687171a5e1a
 * changed to allow for actions to happen to the items prior to checks and for offset
 */
public class RecyclerViewInteraction<A> {

    public interface InclusionFilter<A> {
        boolean include(A item);
    }

    private Matcher<View> viewMatcher;
    private int offset = 0;
    private List<A> items;
    private List<ViewAction> onItemViewActions = new ArrayList<>();
    private InclusionFilter<A> inclusionFilter = new InclusionFilter<A>() {
        @Override
        public boolean include(A item) {
            return true;
        }
    };

    private RecyclerViewInteraction(Matcher<View> viewMatcher) {
        this.viewMatcher = viewMatcher;
    }

    public static <A> RecyclerViewInteraction<A> onRecyclerView(Matcher<View> viewMatcher) {
        return new RecyclerViewInteraction<>(viewMatcher);
    }

    public RecyclerViewInteraction<A> withOffset(int offset) {
        this.offset = offset >= 0 ? offset : 0;
        return this;
    }

    public RecyclerViewInteraction<A> withItemInclusionFilter(InclusionFilter<A> inclusionFilter) {
        this.inclusionFilter = inclusionFilter == null ? this.inclusionFilter : inclusionFilter;
        return this;
    }

    public RecyclerViewInteraction<A> withItems(List<A> items) {
        this.items = items;
        return this;
    }

    public RecyclerViewInteraction<A> check(ItemViewAssertion<A> itemViewAssertion) {
        for (int i = 0; i < items.size(); i++) {
            if (!inclusionFilter.include(items.get(i))) {
                continue;
            }
            ViewInteraction vi = onView(viewMatcher).perform(scrollToPosition(i + offset));
            for (ViewAction viewAction : onItemViewActions) {
                vi.perform(actionOnItemAtPosition(i + offset, viewAction));
            }
            vi.check(new RecyclerItemViewAssertion<>(i + offset, items.get(i), itemViewAssertion));
        }
        return this;
    }

    public RecyclerViewInteraction<A> withOnItemActions(ViewAction... viewActions) {
        onItemViewActions.addAll(Arrays.asList(viewActions));
        return this;
    }

    public interface ItemViewAssertion<A> {
        void check(A item, View view, NoMatchingViewException e);
    }
}