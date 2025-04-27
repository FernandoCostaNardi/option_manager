import * as React from "react";
import * as TabsPrimitive from "@radix-ui/react-tabs";

const Tabs = TabsPrimitive.Root;

const TabsList = React.forwardRef<
  React.ElementRef<typeof TabsPrimitive.List>,
  React.ComponentPropsWithoutRef<typeof TabsPrimitive.List>
>(({ className, ...props }, ref) => (
  <TabsPrimitive.List
    ref={ref}
    className={`inline-flex h-14 items-center justify-center rounded-lg bg-gray-100 p-2 gap-3 text-gray-600 shadow-md border border-gray-200 ${className}`}
    {...props}
  />
));
TabsList.displayName = TabsPrimitive.List.displayName;

const TabsTrigger = React.forwardRef<
  React.ElementRef<typeof TabsPrimitive.Trigger>,
  React.ComponentPropsWithoutRef<typeof TabsPrimitive.Trigger>
>(({ className, ...props }, ref) => (
  <TabsPrimitive.Trigger
    ref={ref}
    className={`
      inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-md px-5 py-2.5 
      text-sm font-medium transition-all duration-200 ease-in-out
      focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2 
      disabled:pointer-events-none disabled:opacity-50 
      ${props['data-state'] === 'active' 
        ? 'bg-blue-600 text-white shadow-lg translate-y-[-2px]' 
        : 'bg-white text-gray-700 hover:bg-gray-100 hover:text-gray-900'} 
      ${className}`}
    {...props}
  >
    <div className="flex items-center gap-2 relative">
      {props.children}
      {props['data-state'] === 'active' && (
        <span className="w-2.5 h-2.5 rounded-full bg-green-400 animate-pulse"></span>
      )}
    </div>
    {props['data-state'] === 'active' && (
      <div className="absolute -bottom-2 left-0 w-full h-1 bg-blue-400 rounded"></div>
    )}
  </TabsPrimitive.Trigger>
));
TabsTrigger.displayName = TabsPrimitive.Trigger.displayName;

const TabsContent = React.forwardRef<
  React.ElementRef<typeof TabsPrimitive.Content>,
  React.ComponentPropsWithoutRef<typeof TabsPrimitive.Content>
>(({ className, ...props }, ref) => (
  <TabsPrimitive.Content
    ref={ref}
    className={`mt-6 ring-offset-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2 transition-all duration-300 animate-in fade-in-50 ${className}`}
    {...props}
  />
));
TabsContent.displayName = TabsPrimitive.Content.displayName;

export { Tabs, TabsList, TabsTrigger, TabsContent };