import * as React from "react"

interface SwitchProps {
  id?: string;
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
  disabled?: boolean;
  className?: string;
}

const Switch = React.forwardRef<HTMLDivElement, SwitchProps>(
  ({ id, checked, onCheckedChange, disabled, className = "" }, ref) => {
    return (
      <div 
        ref={ref}
        id={id}
        role="switch"
        aria-checked={checked}
        data-state={checked ? "checked" : "unchecked"}
        onClick={disabled ? undefined : () => onCheckedChange(!checked)}
        className={`${className} ${disabled ? "opacity-50 cursor-not-allowed" : "cursor-pointer"}`}
        style={{
          position: 'relative',
          display: 'inline-flex',
          alignItems: 'center',
          width: '44px',
          height: '24px',
          borderRadius: '12px',
          backgroundColor: checked ? '#6E56CF' : '#E2E2E2',
          border: '1px solid',
          borderColor: checked ? '#6E56CF' : '#D1D1D1',
          transition: 'background-color 150ms',
          WebkitTapHighlightColor: 'rgba(0, 0, 0, 0)',
        }}
        tabIndex={disabled ? undefined : 0}
      >
        <span
          style={{
            position: 'absolute',
            top: '2px',
            left: '2px',
            width: '18px',
            height: '18px',
            borderRadius: '9px',
            backgroundColor: 'white',
            boxShadow: '0 1px 2px rgba(0, 0, 0, 0.3)',
            transform: checked ? 'translateX(20px)' : 'translateX(0)',
            transition: 'transform 150ms',
          }}
        />
      </div>
    );
  }
);

Switch.displayName = "Switch";

export { Switch };
