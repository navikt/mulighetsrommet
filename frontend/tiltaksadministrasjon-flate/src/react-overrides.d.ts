import React from "react";

declare module "react" {
  /**
   * Redefinerer typedeklarasjonen til `React.forwardRef` etter oppskrift fra følgende artikkel [0].
   *
   * Dette gjør at `forwardRef` støtter generiske props og ble lagt til ifm. at Select-komponenten
   * vi benytter i skjemaer skulle støtte object-options.
   *
   * Denne typedeklarasjonen gjør at vi mister muligheten til å bl.a. sette `displayName` på noden returnert
   * av `forwardRef` - men dette kan løses ved at benytter en named function i stedet for arrow function når
   * man deklarerer komponenten/render-funksjonen som sendes til `forwardRef`.
   *
   * [0]: https://fettblog.eu/typescript-react-generic-forward-refs/
   */
  function forwardRef<T, P = object>(
    render: (props: P, ref: React.Ref<T>) => React.ReactNode | null,
  ): (props: P & React.RefAttributes<T>) => React.ReactNode | null;
}
