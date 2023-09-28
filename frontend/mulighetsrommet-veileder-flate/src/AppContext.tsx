import { QueryClient, QueryClientProvider } from "react-query";
import { ReactQueryDevtools } from "react-query/devtools";
import { FnrContext } from "./hooks/useFnr";
import React, { Dispatch, ReactNode, useEffect, useState } from "react";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      useErrorBoundary: true,
      refetchOnWindowFocus: import.meta.env.PROD,
      retry: import.meta.env.PROD,
    },
  },
});

export interface AppContextProps {
  fnr: string | null;
  setFnrRef?: (setFnr: Dispatch<string>) => void;
  children: ReactNode;
}

export function AppContext(props: AppContextProps) {
  const [fnr, setFnr] = useState(props.fnr);

  useEffect(() => {
    if (props.setFnrRef) {
      props.setFnrRef(setFnr);
    }
  }, []);

  return (
    <QueryClientProvider client={queryClient}>
      <FnrContext.Provider value={fnr}>{props.children}</FnrContext.Provider>
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  );
}
