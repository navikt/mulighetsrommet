import { FunctionComponent, ReactNode, useEffect, useState } from "react";
import { CacheProvider, EmotionCache } from "@emotion/react";

export interface Props {
  cache: EmotionCache;
  children: ReactNode;
}

// react-select css forsvinner når man bruker webcomponent og putter appen i shadowdom. Denne
// hacken fikser det (ikke spør hvordan eller hvorfor).
// se f. eks https://github.com/JedWatson/react-select/issues/3680 og
// https://github.com/emotion-js/emotion/issues/3071#issuecomment-1623831600 for mer info
export const CustomEmotionCacheProvider: FunctionComponent<Props> = ({ cache, children }) => {
  const [isFirstRender, setIsFirstRender] = useState(true);

  useEffect(() => setIsFirstRender(false), []);

  return <CacheProvider value={cache}>{!isFirstRender && children}</CacheProvider>;
};
