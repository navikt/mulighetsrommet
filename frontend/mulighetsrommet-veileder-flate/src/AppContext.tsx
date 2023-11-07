import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { FnrContext } from "./hooks/useFnr";
import { Dispatch, ReactNode, useEffect, useState } from "react";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      throwOnError: true,
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
