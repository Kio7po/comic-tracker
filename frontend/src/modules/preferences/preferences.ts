// The preferences dialog is open exactly while the URL carries this fragment. A fragment (rather
// than a search param) leaves pages that drive their own filters through useSearchParams untouched,
// and a hash-only change never re-runs route loaders.
export const PREFERENCES_HASH = '#preferences';
