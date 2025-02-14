export type LoaderData<TLoader extends (...args: any[]) => any> = Awaited<ReturnType<TLoader>>;
