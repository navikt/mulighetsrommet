export function DemoImageHeader() {
  return import.meta.env.DEV ? (
    <img
      src="/interflatedekorator_arbmark.png"
      id="veilarbpersonflatefs-root"
      alt="veilarbpersonflate-bilde"
      className="w-full h-[174px]"
    />
  ) : null;
}
