export function useNavigerTilAvtale() {
  const navigerTilAvtale = (avtaleId: String) => {
    const origin = window.location.origin;
    window.location.href = `${origin}/avtaler/${avtaleId}`;
  };
  return { navigerTilAvtale };
}
