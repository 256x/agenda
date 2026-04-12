" literalagenda.vim - Vim integration for Literal Agenda
" Place in ~/.vim/plugin/ or add to your vimrc with: source /path/to/literalagenda.vim
"
" Set these to your actual directories:
let g:literalagenda_events    = expand('~/path/to/literalagenda/events/')
let g:literalagenda_repeating = expand('~/path/to/literalagenda/repeating/')

function! LiteralAgendaNew()
  let l:date  = strftime('%Y-%m-%d')
  let l:date  = input('Date (YYYY-MM-DD): ', l:date)
  let l:time  = input('Time (HH:MM, blank for all-day): ')
  let l:rep   = input('Repeat (none/weekly/monthly/yearly): ', 'none')

  let l:file  = g:literalagenda_events . strftime('%Y%m%d_%H%M%S') . '.md'
  let l:lines = ['---', 'date: ' . l:date]
  if !empty(l:time)
    call add(l:lines, 'time: ' . l:time)
  endif
  call extend(l:lines, ['repeat: ' . l:rep, '---', ''])

  call writefile(l:lines, l:file)
  execute 'edit ' . fnameescape(l:file)
  normal! G
endfunction

function! LiteralAgendaList()
  execute 'Explore ' . fnameescape(g:literalagenda_events)
endfunction

function! LiteralAgendaSearch()
  let l:pat = input('Search: ')
  if empty(l:pat) | return | endif
  execute 'vimgrep /' . escape(l:pat, '/') . '/j ' . fnameescape(g:literalagenda_events) . '**/*.md'
  copen
endfunction

nnoremap <leader>an :call LiteralAgendaNew()<CR>
nnoremap <leader>al :call LiteralAgendaList()<CR>
nnoremap <leader>as :call LiteralAgendaSearch()<CR>
