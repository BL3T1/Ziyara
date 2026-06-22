import { useState, type InputHTMLAttributes, type ReactNode } from 'react'
import { Eye, EyeOff } from 'lucide-react'

interface PasswordInputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> {
  wrapperClassName?: string
  leftSlot?: ReactNode
}

export function PasswordInput({
  wrapperClassName,
  leftSlot,
  className = '',
  style,
  ...props
}: PasswordInputProps) {
  const [show, setShow] = useState(false)

  return (
    <div className={`relative ${wrapperClassName ?? ''}`.trim()}>
      {leftSlot}
      <input
        {...props}
        type={show ? 'text' : 'password'}
        className={className}
        style={{ ...style, paddingRight: '2.5rem' }}
      />
      <button
        type="button"
        onClick={() => setShow((s) => !s)}
        tabIndex={-1}
        aria-label={show ? 'Hide password' : 'Show password'}
        className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 focus:outline-none dark:hover:text-slate-300"
      >
        {show ? <EyeOff size={16} /> : <Eye size={16} />}
      </button>
    </div>
  )
}
