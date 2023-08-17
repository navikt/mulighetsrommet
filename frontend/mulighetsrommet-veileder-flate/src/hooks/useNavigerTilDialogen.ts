export function useNavigerTilDialogen() {
  const navigerTilDialogen = (dialogId: String) => {
    const origin = window.location.origin;
    window.location.href = `${origin}/${dialogId}#visDialog`;
  };

  const getUrlTilDialogen = (dialogId: String) => {
    const origin = window.location.origin;
    return `${origin}/${dialogId}#visDialog`;
  };

  return { navigerTilDialogen, getUrlTilDialogen };
}
