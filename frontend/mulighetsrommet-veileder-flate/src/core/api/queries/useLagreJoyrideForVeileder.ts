import { useAtom } from "jotai";
import { JoyrideType } from "mulighetsrommet-api-client";
import { useEffect } from "react";
import { JoyrideStorage, joyrideAtom } from "../../atoms/atoms";
import { mulighetsrommetClient } from "../clients";

export function useLagreJoyrideForVeileder(
  joyrideType: JoyrideType,
  joyrideLocalStorage: keyof JoyrideStorage,
) {
  const [joyride] = useAtom(joyrideAtom);

  useEffect(() => {
    const lagreKjortJoyride = async (joyrideType: JoyrideType) => {
      await mulighetsrommetClient.joyride.lagreJoyrideHarKjort({
        requestBody: { fullfort: true, joyrideType },
      });
    };

    const harKjortJoyride = !joyride[joyrideLocalStorage];
    if (harKjortJoyride) {
      lagreKjortJoyride(joyrideType);
    }
  }, [joyride, joyrideType, joyrideLocalStorage]);
}
