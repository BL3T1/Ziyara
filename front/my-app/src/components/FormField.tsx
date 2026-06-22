import type { ReactNode } from 'react'

interface FormFieldProps {
  label: string
  required?: boolean
  hint?: string
  error?: string
  className?: string
  children: ReactNode
}

export function FormField({ label, required, hint, error, className, children }: FormFieldProps) {
  return (
    <div className={`form-field${className ? ` ${className}` : ''}`}>
      <label className="form-field-label">
        {label}
        {required && (
          <span className="ml-0.5 text-red-500" aria-hidden="true">
            *
          </span>
        )}
      </label>
      {hint && <p className="form-field-hint">{hint}</p>}
      {children}
      {error && (
        <p className="form-field-error" role="alert">
          {error}
        </p>
      )}
    </div>
  )
}
