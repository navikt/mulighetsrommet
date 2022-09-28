export function useNavigerTilDialogen() {
  const navigerTilDialogen = (fnr: String, dialogId: String) => {
    const origin = window.location.origin;
    window.location.href = `${origin}/${fnr}/${dialogId}#visDialog`;
  };

  const getUrlTilDialogen = (fnr: String, dialogId: String) => {
    const origin = window.location.origin;
    return `${origin}/${fnr}/${dialogId}#visDialog`;
  };

  return { navigerTilDialogen, getUrlTilDialogen };
}
