import { atom, WritableAtom } from "jotai";
import {
  atomWithStorage,
  createJSONStorage,
  unstable_withStorageValidator as withStorageValidator,
} from "jotai/utils";

export type FilterState<T> = {
  filter: FilterEntry<T>;
  defaultFilter: FilterEntry<T>;
  isInitialized: boolean;
};

export type FilterEntry<T> = {
  id?: string;
  values: T;
};

export type FilterAction<T> =
  | { type: "UPDATE_DEFAULT"; payload?: FilterEntry<T> }
  | { type: "RESET_TO_DEFAULT" }
  | { type: "SELECT_FILTER"; payload: FilterEntry<T> }
  | { type: "SET_FILTER"; payload: T }
  | { type: "UPDATE_FILTER"; payload: Partial<T> };

export function createFilterStateAtom<T extends object>(
  storageKey: string,
  fallbackFilter: T,
  validator: (value: unknown) => value is T,
): WritableAtom<FilterState<T>, [FilterAction<T>], void> {
  const initialState = {
    isInitialized: false,
    defaultFilter: { id: undefined, values: fallbackFilter },
    filter: { values: fallbackFilter },
  };

  const filterStateStorage = withStorageValidator<FilterState<T>>(
    (value: unknown): value is FilterState<T> => {
      if (!value || typeof value !== "object") return false;
      const state = value as any;
      return (
        state.filter &&
        validator(state.filter.values) &&
        (typeof state.filter.id === "string" || state.filter.id === undefined) &&
        (state.defaultFilter === undefined || validator(state.defaultFilter.values)) &&
        typeof state.isInitialized === "boolean"
      );
    },
  )(createJSONStorage(() => sessionStorage));

  function filterReducer(state: FilterState<T>, action: FilterAction<T>): FilterState<T> {
    switch (action.type) {
      case "UPDATE_DEFAULT": {
        const defaultFilter = action.payload ?? initialState.defaultFilter;
        if (!state.isInitialized) {
          return {
            ...state,
            isInitialized: true,
            filter: defaultFilter,
            defaultFilter,
          };
        }

        return { ...state, defaultFilter };
      }

      case "RESET_TO_DEFAULT":
        return {
          ...state,
          filter: state.defaultFilter,
        };

      case "SET_FILTER":
        return {
          ...state,
          filter: {
            id: undefined,
            values: action.payload,
          },
        };

      case "UPDATE_FILTER":
        return {
          ...state,
          filter: {
            id: undefined,
            values: { ...state.filter.values, ...action.payload },
          },
        };

      case "SELECT_FILTER":
        return {
          ...state,
          filter: action.payload,
        };

      default:
        return state;
    }
  }

  const filterManagerAtom = atomWithStorage<FilterState<T>>(
    storageKey,
    initialState,
    filterStateStorage,
    { getOnInit: true },
  );

  return atom(
    (get) => get(filterManagerAtom),
    (get, set, action: FilterAction<T>) => {
      set(filterManagerAtom, filterReducer(get(filterManagerAtom), action));
    },
  );
}
